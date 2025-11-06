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
		log.Printf("Invalid email format: %s from %s", email, r.RemoteAddr)
		return
	}

	sanitizedEmail := sanitizeEmail(email)

	err := unsubscribeEmail(sanitizedEmail)
	if err != nil {
		log.Printf("Error unsubscribing email %s: %v", sanitizedEmail, err)
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintln(w, "Successfully unsubscribed.")
	log.Printf("Successfully unsubscribed %s from %s", sanitizedEmail, r.RemoteAddr)
}

func isValidEmail(email string) bool {
	// Use the net/mail package for more robust email validation
	_, err := mail.ParseAddress(email)
	return err == nil
}

func sanitizeEmail(email string) string {
	sanitized := url.QueryEscape(email)
	return sanitized
}

func unsubscribeEmail(email string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	// Improved error handling: check if the email exists before attempting to update
	var exists bool
	err := db.QueryRowContext(ctx, "SELECT EXISTS (SELECT 1 FROM subscribers WHERE email = $1)", email).Scan(&exists)
	if err != nil {
		return fmt.Errorf("failed to check email existence")
	}

	if !exists {
		return fmt.Errorf("email not found")
	}

	result, err := db.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = TRUE WHERE email = $1", email)
	if err != nil {
		return fmt.Errorf("failed to update subscription status")
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected")
	}

	if rowsAffected == 0 {
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