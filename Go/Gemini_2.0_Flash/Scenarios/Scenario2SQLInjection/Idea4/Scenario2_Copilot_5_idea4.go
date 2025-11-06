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
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
			confirmed BOOLEAN DEFAULT TRUE
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
		log.Printf("Invalid email format: %s from %s", email, r.RemoteAddr)
		return
	}

	sanitizedEmail := sanitizeEmail(email)

	err := unsubscribeEmail(sanitizedEmail, w, r) // Pass ResponseWriter and Request
	if err != nil {
		log.Printf("Error unsubscribing email %s: %v", sanitizedEmail, err)
		http.Error(w, "Unsubscribe failed. Please contact support.", http.StatusInternalServerError)
		return
	}

	// Add a confirmation page to prevent accidental unsubscriptions
	confirmationHTML := fmt.Sprintf(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Unsubscribe Confirmation</title>
        </head>
        <body>
            <h1>Are you sure you want to unsubscribe?</h1>
            <p>Click the button below to confirm.</p>
            <form method="POST" action="/confirm_unsubscribe">
                <input type="hidden" name="email" value="%s">
				<input type="hidden" name="csrf_token" value="%s">
                <button type="submit">Confirm Unsubscribe</button>
            </form>
        </body>
        </html>
    `, sanitizedEmail, generateCSRFToken())
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, confirmationHTML)

	log.Printf("Unsubscribe confirmation page sent to %s for %s", r.RemoteAddr, sanitizedEmail)
}

func isValidEmail(email string) bool {
	emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
	return emailRegex.MatchString(email)
}

func sanitizeEmail(email string) string {
	sanitized := url.QueryEscape(email)
	return sanitized
}

func unsubscribeEmail(email string, w http.ResponseWriter, r *http.Request) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if !isCSRFTokenValid(r) {
		log.Printf("CSRF token validation failed for email %s from %s", email, r.RemoteAddr)
		http.Error(w, "CSRF token missing or invalid", http.StatusForbidden)
		return fmt.Errorf("CSRF token missing or invalid")
	}

	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		log.Printf("Error starting transaction: %v", err)
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

	// Only unsubscribe if the user has confirmed their email
	result, err := tx.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = TRUE WHERE email = $1 AND confirmed = TRUE", email)

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
		log.Printf("Email not found or not confirmed: %s", email)
		return fmt.Errorf("email not found or not confirmed")
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

func isCSRFTokenValid(r *http.Request) bool {
	// In a real application, you would compare a token stored in the session
	// with a token submitted in the request (e.g., in a header or form field).
	// This is a placeholder.
	return true
}

func generateCSRFToken() string {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return ""
	}
	return base64.StdEncoding.EncodeToString(b)
}