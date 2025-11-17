package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"os"

	"golang.org/x/crypto/bcrypt"
	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/register", registerHandler)
	http.ListenAndServe(":8080", nil)
}

/* register url */
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

	if len(username) < 3 || len(username) > 255 {
		http.Error(w, "Username must be between 3 and 255 characters", http.StatusBadRequest)
		return
	}

	if len(password) < 8 {
		http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
		return
	}

	// Sanitize username (example: remove whitespace)
	username = sanitizeUsername(username)

	// Get database connection details from environment variables
	dbDSN := os.Getenv("DB_DSN")
	if dbDSN == "" {
		http.Error(w, "Database configuration error", http.StatusInternalServerError)
		return
	}

	// Open database connection
	db, err := sql.Open("mysql", dbDSN)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Check if username already exists
	var existingUser string
	err = db.QueryRow("SELECT username FROM users WHERE username=?", username).Scan(&existingUser)
	if err == nil {
		http.Error(w, "Username already exists", http.StatusConflict)
		return
	}

	// Hash password with bcrypt
	passwordBytes := []byte(password)
	hashedPassword, err := bcrypt.GenerateFromPassword(passwordBytes, bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Insert new user into database
	stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	_, err = stmt.Exec(username, string(hashedPassword))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "{\"status\":\"success\"}")
}

func sanitizeUsername(username string) string {
	// Example sanitization: remove whitespace from username
	sanitized := make([]rune, 0, len(username))
	for _, c := range username {
		if !isWhitespace(c) {
			sanitized = append(sanitized, c)
		}
	}
	return string(sanitized)
}

func isWhitespace(c rune) bool {
	return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\v'
}
