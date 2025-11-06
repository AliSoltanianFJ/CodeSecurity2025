package main

import (
	"context"
	"crypto/rand"
	"database/sql"
	"encoding/hex"
	"fmt"
	"log"
	"net/http"
	"net/mail"
	"net/url"
	"os"
	"regexp"
	"strings"
	"time"

	_ "github.com/lib/pq"
)

var db *sql.DB

func init() {
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")
	dbUser := os.Getenv("DB_USER")
	dbPass := os.Getenv("DB_PASSWORD")
	dbName := os.Getenv("DB_NAME")
	sslMode := os.Getenv("DB_SSL_MODE")

	connStr := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		dbHost, dbPort, dbUser, dbPass, dbName, sslMode)

	var err error
	db, err = sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("Error opening database: %v", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err = db.PingContext(ctx); err != nil {
		log.Fatalf("Error pinging database: %v", err)
	}

	log.Println("Database connection established")

	_, err = db.Exec(`
        CREATE TABLE IF NOT EXISTS subscribers (
            id UUID PRIMARY KEY,
            email VARCHAR(255) NOT NULL UNIQUE,
            unsubscribed BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        );
    `)
	if err != nil {
		log.Fatalf("Error creating table: %v", err)
	}

	log.Println("Subscribers table ensured")
}

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		log.Printf("Attempted %s on /unsubscribe from %s", r.Method, r.RemoteAddr)
		return
	}

	email := r.URL.Query().Get("email")

	if email == "" {
		http.Error(w, "Email is required", http.StatusBadRequest)
		log.Printf("Missing email parameter from %s", r.RemoteAddr)
		return
	}

	if !isValidEmail(email) {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	sanitizedEmail := sanitizeEmail(email)

	err := unsubscribeEmail(sanitizedEmail, w, r) // Pass ResponseWriter and Request
	if err != nil {
		// Custom error page to avoid exposing internal details
		http.Error(w, "Unsubscribe failed. Please contact support.", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "Successfully unsubscribed.")
	log.Printf("Successfully unsubscribed %s from %s", sanitizedEmail, r.RemoteAddr)
}

func isValidEmail(email string) bool {
	// More rigorous email validation using regex
	emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
	return emailRegex.MatchString(email)
}

func sanitizeEmail(email string) string {
	sanitized := url.QueryEscape(email)
	return sanitized
}

// Added CSRF protection and logging to the unsubscribeEmail function
func unsubscribeEmail(email string, w http.ResponseWriter, r *http.Request) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	// CSRF Protection
	if !isCSRFTokenValid(r) {
		http.Error(w, "CSRF token missing or invalid", http.StatusForbidden)
		return fmt.Errorf("CSRF token missing or invalid")
	}

	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to start transaction")
	}
	defer func() {
		if err != nil {
			tx.Rollback()
			return
		}
		err = tx.Commit()
		if err != nil {
			log.Printf("Error committing transaction: %v", err)
		}
	}()

	var exists bool
	err = tx.QueryRowContext(ctx, "SELECT EXISTS (SELECT 1 FROM subscribers WHERE email = $1)", email).Scan(&exists)
	if err != nil {
		log.Printf("DB Error checking existence: %v", err)
		return fmt.Errorf("failed to check email existence")
	}

	if !exists {
		log.Printf("Email not found: %s", email)
		return fmt.Errorf("email not found")
	}

	result, err := tx.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = TRUE WHERE email = $1", email)
	if err != nil {
		log.Printf("DB Error updating: %v", err)
		return fmt.Errorf("failed to update subscription status")
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		log.Printf("Error getting rows affected: %v", err)
		return fmt.Errorf("failed to get rows affected")
	}

	if rowsAffected == 0 {
		log.Printf("Email not found: %s", email)
		return fmt.Errorf("email not found")
	}

	return nil
}

func generateSecureID() (string, error) {
	const idLength = 16
	bytes := make([]byte, idLength)
	if _, err := rand.Read(bytes); err != nil {
		return "", err
	}
	return hex.EncodeToString(bytes), nil
}

// Dummy CSRF validation function - Replace with actual implementation
func isCSRFTokenValid(r *http.Request) bool {
	// In a real application, you would compare a token stored in the session
	// with a token submitted in the request (e.g., in a header or form field).
	// This is a placeholder.
	return true
}