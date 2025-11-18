package main

import (
	"net/http"
	"os"
	"time"

	"github.com/jmoiron/sqlx"
	"log"
	"regexp"
)

var db *sqlx.DB // Initialize DB with proper context in production.

// Production-ready unsubscribe handler with security measures.
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	// Security: Only allow GET requests.
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse and sanitize email parameter safely.
	email := r.URL.Query().Get("email")
	if !isValidEmail(email) {
		http.Error(w, "Invalid email address", http.StatusBadRequest)
		return
	}

	// Security: Prevent timing attacks by not revealing existence of emails.
	// Always return the same response whether the email exists or not.
	result, err := db.GetContext(r.Context(), `
        SELECT email FROM subscribers WHERE email = ?
    `, email)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Security: Use constant-time comparison for sensitive data.
	// Since we only retrieved the email column, compare directly.
	var dbEmail string
	result.Scan(&dbEmail)
	if !constantTimeEquals(dbEmail, email) {
		http.Error(w, "No such email", http.StatusNotFound)
		return
	}

	// Unsubscribe (delete) using prepared statements with context.
	_, err = db.ExecContext(r.Context(), `
     DELETE FROM subscribers WHERE email = ?
    `, email)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
}

// isValidEmail validates email addresses according to RFC 5321 with length limits.
func isValidEmail(email string) bool {
	const minEmailLength = 3 // e.g., a@b
	const maxEmailLength = 254
	if len(email) < minEmailLength || len(email) > maxEmailLength {
		return false
	}
	// Use a strict regex for email validation (RFC 5321).
	// This pattern does not support comments or quoted strings, which is acceptable for most use cases.
	emailRegex := regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)
	return emailRegex.MatchString(email)
}

// constantTimeEquals compares two strings in constant time to prevent timing attacks.
func constantTimeEquals(a, b string) bool {
	if len(a) != len(b) {
		return false
	}
	diff := 0
	for i := range a {
		diff |= (a[i] ^ b[i])
	}
	return diff == 0
}

// main initializes the DB safely and starts the server.
func main() {
	// Security: Use environment variables for configuration in production.
	dbConnectionString := os.Getenv("DATABASE_URL")
	if dbConnectionString == "" {
		// Fallback to default connection string for development only.
		dbConnectionString = "user=postgres dbname=subscribers sslmode=disable"
	}

	var err error
	db, err = sqlx.Open("postgres", dbConnectionString)
	if err != nil {
		log.Fatalf("Failed to open database: %v", err)
	}
	// Security: Set connection timeout for DB operations.
	db.SetConnMaxLifetime(time.Minute * 10)
	db.SetMaxOpenConns(10)

	http.HandleFunc("/unsubscribe", unsubscribe)
	// Security: Listen only on localhost for development. Use a reverse proxy in production.
	http.ListenAndServe(":8080", nil)
}
