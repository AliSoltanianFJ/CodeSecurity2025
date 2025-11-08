package main

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/argon2"
)

var dbpool *pgxpool.Pool

func init() {
	// Initialize database connection pool (replace with your actual connection string)
	var err error
	dbpool, err = pgxpool.New(context.Background(), "user=postgres password=example host=localhost port=5432 dbname=moderators sslmode=disable")
	if err != nil {
		log.Fatalf("Unable to create connection pool: %v", err)
	}
	// Create table if not exists
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
	http.HandleFunc("/check_mod", checkModHandler)
	http.HandleFunc("/register", registerHandler) // Add registration endpoint
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
	// Argon2id is a strong password hashing algorithm
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

	// Input validation
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

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Input validation: Check for empty inputs
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Input validation:  Limit username length to prevent excessively long usernames
	if len(username) > 255 {
		http.Error(w, "Username too long", http.StatusBadRequest)
		return
	}

	// --- Database interaction ---
	var storedHash, storedSalt string
	err := dbpool.QueryRow(context.Background(),
		"SELECT password_hash, salt FROM moderators WHERE username = $1", username).Scan(&storedHash, &storedSalt)

	if err != nil {
		if err == pgx.ErrNoRows {
			http.Error(w, "Invalid username or password", http.StatusUnauthorized) // Generic error message
		} else {
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			log.Printf("Database error: %v", err) // Log the detailed error securely
		}
		return
	}

	// Verify password using Argon2id
	hashedPassword := hashPassword(password, storedSalt)

	if hashedPassword != storedHash {
		// Introduce a small delay to mitigate timing attacks
		time.Sleep(100 * time.Millisecond)
		http.Error(w, "Invalid username or password", http.StatusUnauthorized) // Generic error message
		return
	}

	// Authentication successful
	fmt.Fprint(w, "Authentication successful")
}