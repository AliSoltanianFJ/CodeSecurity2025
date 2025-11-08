package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/argon2"
	"golang.org/x/time/rate"
)

var (
	dbPool      *pgxpool.Pool
	loginLimits = make(map[string]*rate.Limiter)
	mu          sync.Mutex
	maxLoginAttempts = 3
	lockoutDuration = 1 * time.Minute
)


func init() {
	var err error
	dbPool, err = pgxpool.New(context.Background(), "user=postgres password=example host=localhost port=5432 dbname=moderators sslmode=disable")
	if err != nil {
		log.Fatalf("Unable to create connection pool: %v", err)
	}

	_, err = dbPool.Exec(context.Background(), `
		CREATE TABLE IF NOT EXISTS moderators (
			username VARCHAR(255) PRIMARY KEY,
			password_hash VARCHAR(255),
			salt VARCHAR(255),
			login_attempts INT DEFAULT 0,
			lockout_until TIMESTAMP
		);
	`)
	if err != nil {
		log.Fatalf("Unable to create table: %v", err)
	}
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.HandleFunc("/register", registerHandler)
	fmt.Println("Server listening on :8080")
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func generateSalt() (string, error) {
	bytes := make([]byte, 16)
	if _, err := rand.Read(bytes); err != nil {
		return "", err
	}
	return hex.EncodeToString(bytes), nil
}

func hashPassword(password, salt string) string {
	hashedPassword := argon2.IDKey([]byte(password), []byte(salt), 1, 64*1024, 4, 32)
	return hex.EncodeToString(hashedPassword)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if len(username) > 255 {
		http.Error(w, "Username too long", http.StatusBadRequest)
		return
	}

	if len(password) < 8 {
		http.Error(w, "Password must be at least 8 characters long", http.StatusBadRequest)
		return
	}

	salt, err := generateSalt()
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("Error generating salt: %v", err)
		return
	}

	hashedPassword := hashPassword(password, salt)

	_, err = dbPool.Exec(context.Background(),
		"INSERT INTO moderators (username, password_hash, salt) VALUES ($1, $2, $3)", username, hashedPassword, salt)

	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("Error inserting user: %v", err)
		return
	}

	fmt.Fprint(w, "Registration successful")
}

func getLoginLimiter(username string) *rate.Limiter {
	mu.Lock()
	defer mu.Unlock()

	limiter, exists := loginLimits[username]
	if !exists {
		limiter = rate.NewLimiter(rate.Limit(maxLoginAttempts), maxLoginAttempts)
		loginLimits[username] = limiter
	}
	return limiter
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if len(username) > 255 {
		http.Error(w, "Username too long", http.StatusBadRequest)
		return
	}

	limiter := getLoginLimiter(username)
	if !limiter.Allow() {
		http.Error(w, "Too many login attempts. Please try again later.", http.StatusTooManyRequests)
		return
	}

	var storedHash, storedSalt string
	var loginAttempts int
	var lockoutUntil time.Time

	err := dbPool.QueryRow(context.Background(),
		"SELECT password_hash, salt, login_attempts, lockout_until FROM moderators WHERE username = $1", username).Scan(&storedHash, &storedSalt, &loginAttempts, &lockoutUntil)

	if err != nil {
		if err == pgx.ErrNoRows {
			http.Error(w, "Invalid username or password", http.StatusUnauthorized)
		} else {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			log.Printf("Database error: %v", err)
		}

		return
	}

	if lockoutUntil.After(time.Now()) {
		remaining := time.Until(lockoutUntil)
		http.Error(w, fmt.Sprintf("Account locked. Please try again in %v", remaining), http.StatusTooManyRequests)
		return
	}

	hashedPassword := hashPassword(password, storedSalt)

	if hashedPassword != storedHash {
		newAttempts := loginAttempts + 1
		var lockoutTime time.Time
		if newAttempts >= maxLoginAttempts {
			lockoutTime = time.Now().Add(lockoutDuration)
		}

		_, err = dbPool.Exec(context.Background(),
			"UPDATE moderators SET login_attempts = $1, lockout_until = $2 WHERE username = $3", newAttempts, lockoutTime, username)
		if err != nil {
			log.Printf("Failed to update login attempts: %v", err)
		}

		http.Error(w, "Invalid username or password", http.StatusUnauthorized)
		return
	}

	// Reset login attempts on successful login
	_, err = dbPool.Exec(context.Background(),
		"UPDATE moderators SET login_attempts = 0, lockout_until = NULL WHERE username = $1", username)
	if err != nil {
		log.Printf("Failed to reset login attempts: %v", err)
	}

	fmt.Fprint(w, "Authentication successful")
}