package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"
	"time"

	"github.com/gorilla/csrf"
	_ "github.com/lib/pq"
)

var db *sql.DB

func main() {
	initDB()
	defer closeDB()

	key := []byte(os.Getenv("CSRF_AUTH_KEY")) // 32 byte key
	if len(key) != 32 {
		log.Fatal("CSRF_AUTH_KEY must be 32 bytes")
	}

	csrfMiddleware := csrf.Protect(key,
		csrf.Secure(true),          // Requires HTTPS
		csrf.FieldName("csrf_token"), // Name of the hidden field
		csrf.HttpOnly(true),         // Mitigates XSS
		csrf.SameSite(csrf.SameSiteStrictMode),
	)

	http.HandleFunc("/unsubscribe", csrfMiddleware(unsubscribeHandler).ServeHTTP)
	http.HandleFunc("/get_csrf_token", getCSRFToken)

	log.Fatal(http.ListenAndServe(":8080", nil))
}

func initDB() {
	var err error
	dbUser := os.Getenv("DB_USER")
	dbPass := os.Getenv("DB_PASS")
	dbName := os.Getenv("DB_NAME")
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")

	dsn := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=require", dbHost, dbPort, dbUser, dbPass, dbName)

	db, err = sql.Open("postgres", dsn)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	db.SetConnMaxLifetime(time.Minute * 3)
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(10)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	err = db.PingContext(ctx)
	if err != nil {
		log.Fatalf("Failed to ping database: %v", err)
	}

	_, err = db.ExecContext(ctx, `
		CREATE TABLE IF NOT EXISTS subscriptions (
			email VARCHAR(255) PRIMARY KEY
		)
	`)
	if err != nil {
		log.Fatalf("Failed to create table: %v", err)
	}
}

func closeDB() {
	if db != nil {
		err := db.Close()
		if err != nil {
			log.Printf("Error closing database: %v", err)
		}
	}
}

func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	if err := r.ParseForm(); err != nil {
		http.Error(w, "Error parsing form", http.StatusBadRequest)
		return
	}

	email := r.Form.Get("email")
	if email == "" {
		http.Error(w, "Email is required", http.StatusBadRequest)
		return
	}

	decodedEmail, err := url.QueryUnescape(email)
	if err != nil {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	if !isValidEmail(decodedEmail) {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	if err := unsubscribeEmail(decodedEmail); err != nil {
		log.Printf("Unsubscribe error: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"message": fmt.Sprintf("Successfully unsubscribed %s", decodedEmail)})

}

func isValidEmail(email string) bool {
	if len(email) > 254 {
		return false
	}
	for i := 0; i < len(email); i++ {
		if email[i] > 127 {
			return false
		}
	}
	return true
}

func unsubscribeEmail(email string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer func() {
		if err != nil {
			tx.Rollback()
			return
		}
		err = tx.Commit()
		if err != nil {
			log.Printf("Failed to commit transaction: %v", err)
		}
	}()

	stmt, err := tx.PrepareContext(ctx, "DELETE FROM subscriptions WHERE email = ?")
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()

	result, err := stmt.ExecContext(ctx, email)
	if err != nil {
		return fmt.Errorf("failed to execute statement: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("email %s not found in subscriptions", email)
	}

	return nil
}

func getCSRFToken(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"csrf_token": csrf.Token(r)})
}