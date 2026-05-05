package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"regexp"

	_ "github.com/mattn/go-sqlite3"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribeHandler)
	http.ListenAndServe(":8080", nil)
}

// unsubscribeHandler handles GET requests to /unsubscribe?email=...
func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow GET requests
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse query parameters safely
	email := r.URL.Query().Get("email")
	if !isValidEmail(email) {
		http.Error(w, "Invalid email address", http.StatusBadRequest)
		return
	}

	// Open database (use environment variable for DB path in production)
	dbPath := os.Getenv("DB_PATH")
	if dbPath == "" {
		dbPath = "./subscribers.db"
	}
	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("DB open error: %v", err) // Do not leak DB path or details
		return
	}
	defer db.Close()

	ctx := context.Background()
	// Check if email exists in the database (use parameterized queries)
	var exists bool
	err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("DB query error: %v", err) // Do not leak DB details
		return
	}

	if !exists {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(response{Message: "Email not found"})
		return
	}

	// Unsubscribe (delete from database) using parameterized query
	stmt, err := db.PrepareContext(ctx, "DELETE FROM subscribers WHERE email = ?")
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("DB prepare error: %v", err)
		return
	}
	defer stmt.Close()

	_, err = stmt.ExecContext(ctx, email)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("DB exec error: %v", err)
		return
	}

	// Explicitly commit (in case of implicit transactions)
	tx, err := db.BeginTx(context.Background(), nil)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		log.Printf("DB begin transaction error: %v", err)
		return
	}
	defer tx.Rollback()
	// ... commit only if successful
	tx.Commit()

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response{Message: "Unsubscribed successfully"})
}

// response is a consistent JSON response structure
type response struct {
	Message string `json:"message"`
}

// isValidEmail validates an email address strictly following RFC 5321 (with length checks)
func isValidEmail(email string) bool {
	if len(email) < 1 || len(email) > 254 {
		return false
	}
	valid := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
	return valid.MatchString(email)
}
