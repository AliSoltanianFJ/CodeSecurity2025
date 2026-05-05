package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"

	_ "github.com/mattn/go-sqlite3" // Import SQLite driver
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	email, err := getEmailParam(r)
	if err != nil {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	db, err := openDB()
	if err != nil {
		log.Printf("Database error: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	err = unsubscribeEmail(db, email)
	if err != nil {
		log.Printf("Unsubscribe error: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed %s", email)
}

func getEmailParam(r *http.Request) (string, error) {
	email := r.URL.Query().Get("email")
	if email == "" {
		return "", fmt.Errorf("email parameter is required")
	}

	decodedEmail, err := url.QueryUnescape(email)
	if err != nil {
		return "", fmt.Errorf("failed to decode email: %w", err)
	}

	if !isValidEmail(decodedEmail) {
		return "", fmt.Errorf("invalid email format")
	}

	return decodedEmail, nil
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

func openDB() (*sql.DB, error) {
	db, err := sql.Open("sqlite3", "emails.db")
	if err != nil {
		return nil, fmt.Errorf("failed to open database: %w", err)
	}

	_, err = db.Exec(`
		CREATE TABLE IF NOT EXISTS subscriptions (
			email TEXT PRIMARY KEY
		)
	`)
	if err != nil {
		return nil, fmt.Errorf("failed to create table: %w", err)
	}

	return db, nil
}

func unsubscribeEmail(db *sql.DB, email string) error {
	tx, err := db.Begin()
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

	stmt, err := tx.Prepare("DELETE FROM subscriptions WHERE email = ?")
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()

	result, err := stmt.Exec(email)
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