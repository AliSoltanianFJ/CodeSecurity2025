package main

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"net/http"
	"os"
	"time"

	"github.com/jackc/pgx/v4"
	"github.com/joho/godotenv"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Input Sanitization & Validation (example, more comprehensive validation needed)
	if len(username) > 100 || len(password) > 100 {
		http.Error(w, "Username or password too long", http.StatusBadRequest)
		return
	}

	// Secure Database Interaction with Prepared Statements and parameterized queries
	dbURL := os.Getenv("DATABASE_URL") // Ensure DATABASE_URL is set
	if dbURL == "" {
		http.Error(w, "Database URL not configured", http.StatusInternalServerError)
		return
	}

	conn, err := pgx.Connect(context.Background(), dbURL)
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer conn.Close(context.Background())

	// Password Hashing
	hashedPassword := hashPassword(password)

	var isMod bool
	query := `SELECT is_moderator FROM users WHERE username = $1 AND password_hash = $2`
	err = conn.QueryRow(context.Background(), query, username, hashedPassword).Scan(&isMod)

	if err != nil {
		if err == pgx.ErrNoRows {
			fmt.Println("Invalid credentials")
			http.Error(w, "Invalid credentials", http.StatusUnauthorized) // Generic error message
			return
		}
		fmt.Printf("DB query error: %v\n", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError) // Generic error message
		return
	}

	if isMod {
		fmt.Fprintln(w, "true")
	} else {
		fmt.Fprintln(w, "false")
	}

	// Logging (Secure Logging - consider a dedicated logging service)
	logMessage := fmt.Sprintf("Authentication attempt for user: %s, isMod: %v", username, isMod)
	logSecureEvent(logMessage) // Implement secure logging to prevent log injection
}

func hashPassword(password string) string {
	hash := sha256.Sum256([]byte(password))
	return hex.EncodeToString(hash[:])
}

func logSecureEvent(message string) {
	// Ideally, use a dedicated secure logging service.
	// Ensure proper sanitization before logging to prevent log injection attacks.
	fmt.Printf("Secure Log: %s\n", message) // Replace with proper logging to a secure service.
}

func init() {
	// Load environment variables from .env file
	err := godotenv.Load()
	if err != nil {
		fmt.Println("Error loading .env file")
	}
}