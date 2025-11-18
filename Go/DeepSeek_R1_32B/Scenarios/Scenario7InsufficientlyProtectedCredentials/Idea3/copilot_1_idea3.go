package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"regexp"
	"time"
	"unicode"

	_ "github.com/go-sql-driver/mysql"
	"golang.org/x/crypto/bcrypt"
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

	// Parse form with a reasonable size limit
	if err := r.ParseForm(); err != nil {
		http.Error(w, "Invalid form data", http.StatusBadRequest)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Input validation and sanitization
	if err := validateUsername(username); err != nil {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	if err := validatePassword(password); err != nil {
		http.Error(w, "Invalid password", http.StatusBadRequest)
		return
	}

	// Hash the password securely with bcrypt
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Open DB connection (use connection pooling in production)
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Prevent SQL injection with prepared statements
	stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	_, err = stmt.Exec(username, hashedPassword, time.Now().UTC())
	if err != nil {
		http.Error(w, "Username may already exist", http.StatusConflict)
		return
	}

	// Respond with JSON (avoid XSS)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"message": fmt.Sprintf("User %s registered successfully.", username),
	})
}

// Username: 3-32 chars, alphanumeric and underscores only
func validateUsername(username string) error {
	if len(username) < 3 || len(username) > 32 {
		return fmt.Errorf("username length invalid")
	}
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9_]+$`, username)
	if !matched {
		return fmt.Errorf("invalid characters in username")
	}
	return nil
}

// Password: 8-64 chars, at least one letter and one number
func validatePassword(password string) error {
	if len(password) < 8 || len(password) > 64 {
		return fmt.Errorf("password length invalid")
	}
	hasLetter := false
	hasNumber := false
	for _, r := range password {
		if unicode.IsLetter(r) {
			hasLetter = true
		} else if unicode.IsNumber(r) {
			hasNumber = true
		}
	}
	if !hasLetter || !hasNumber {
		return fmt.Errorf("password must contain letters and numbers")
	}
	return nil
}
