package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gorilla/csrf"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/argon2"
)

var dbpool *pgxpool.Pool

func init() {
	var err error
	dbpool, err = pgxpool.New(context.Background(), os.Getenv("DATABASE_URL"))
	if err != nil {
		log.Fatalf("Unable to create connection pool: %v", err)
	}

	_, err = dbpool.Exec(context.Background(), `
		CREATE TABLE IF NOT EXISTS moderators (
			username VARCHAR(255) PRIMARY KEY,
			password_hash VARCHAR(255),
			salt VARCHAR(255)
		);
	`)
	if err != nil {
		log.Fatalf("Unable to create table: %v", err)
	}
}

func main() {
	csrfKey := make([]byte, 32)
	if _, err := rand.Read(csrfKey); err != nil {
		log.Fatal("Error generating CSRF key:", err)
	}

	csrfMiddleware := csrf.Protect(csrfKey, csrf.Secure(false)) // Set Secure(true) in production with HTTPS

	http.HandleFunc("/check_mod", checkModHandler)
	http.HandleFunc("/register", registerHandler)

	// Wrap handlers with CSRF protection
	http.Handle("/check_mod", csrfMiddleware(http.HandlerFunc(checkModHandler)))
	http.Handle("/register", csrfMiddleware(http.HandlerFunc(registerHandler)))

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

	_, err = dbpool.Exec(context.Background(),
		"INSERT INTO moderators (username, password_hash, salt) VALUES ($1, $2, $3)", username, hashedPassword, salt)

	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("Error inserting user: %v", err)
		return
	}

	fmt.Fprint(w, "Registration successful")
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

	var storedHash, storedSalt string
	err := dbpool.QueryRow(context.Background(),
		"SELECT password_hash, salt FROM moderators WHERE username = $1", username).Scan(&storedHash, &storedSalt)

	if err != nil {
		if err == pgx.ErrNoRows {
			http.Error(w, "Invalid username or password", http.StatusUnauthorized)
		} else {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			log.Printf("Database error: %v", err)
		}
		return
	}

	hashedPassword := hashPassword(password, storedSalt)

	if hashedPassword != storedHash {
		time.Sleep(100 * time.Millisecond)
		http.Error(w, "Invalid username or password", http.StatusUnauthorized)
		return
	}

	fmt.Fprint(w, "Authentication successful")
}