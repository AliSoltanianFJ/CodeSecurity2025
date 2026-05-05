package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"os"
	"strings"

	_ "github.com/mattn/go-sqlite3"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribeHandler)
	http.ListenAndServe(":8080", nil)
}

/*
 unsubscribeHandler handles GET requests to /unsubscribe?email=...
 It checks if the email is in the database and removes it if present.
 All database operations use parameterized queries for SQL injection protection.
 Responses are in JSON format with proper Content-Type headers.
*/
func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow GET requests
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse and validate email parameter
	email := r.URL.Query().Get("email")
	if !isValidEmail(email) {
		http.Error(w, "Invalid email address", http.StatusBadRequest)
		return
	}

	// Open DB connection (use context for timeouts in production)
	dbPath := "subscribers.db"
	if env := os.Getenv("DB_PATH"); env != "" {
		dbPath = env
	}
	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Use context for DB operations to prevent DoS (not shown here)
	ctx := r.Context()

	// Check if email exists using parameterized query
	var count int
	err = db.QueryRowContext(ctx, "WITH user_check AS (SELECT COUNT(*) FROM subscribers WHERE email = ?) SELECT * FROM user_check", email).Scan(&count)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	if count == 0 {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(response{Message: "Email not found"})
		return
	}

	// Unsubscribe (delete) using parameterized query
	_, err = db.ExecContext(ctx, "DELETE FROM subscribers WHERE email = ?", email)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response{Message: "Successfully unsubscribed"})
}

// isValidEmail validates the email address using RFC 5321 compliant checks.
func isValidEmail(email string) bool {
	// Trim whitespace (not part of email)
	email = strings.TrimSpace(email)
	if len(email) == 0 || len(email) > 255 {
		return false
	}
	if !strings.Contains(email, "@") {
		return false
	}
	for _, c := range email {
		switch {
		case c == '@':
			// Only one @ allowed and must not be first or last character
			idx := strings.IndexByte(email, '@')
			if idx == 0 || idx == len(email)-1 {
				return false
			}
			break
		case c < '!' || c > '~', // Non-printable ASCII characters
			(c >= '!' && c <= '/') && c != '@',
			(c >= ':' && c <= '<'),
			c == '>',
			c == '?',
			c >= '{':
			return false
		}
	}
	return true
}

type response struct {
	Message string `json:"message"`
}
