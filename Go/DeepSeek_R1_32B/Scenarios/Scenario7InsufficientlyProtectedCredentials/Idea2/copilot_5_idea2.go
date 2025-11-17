package main

import (
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"os"
	"regexp"

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

	// Sanitize and validate username
	username = sanitiseInput(username)
	if !isValidUsername(username) {
		http.Error(w, "Invalid username. Only letters, numbers, . - _ allowed", http.StatusBadRequest)
		return
	}

	// Validate password complexity
	if !isValidPassword(password) {
		http.Error(w, "Weak password. Must be at least 8 characters with uppercase, lowercase, number and special character", http.StatusBadRequest)
		return
	}

	// Database connection
	db, err := sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Generate salt and hash password
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	hashedPassword := sha256.Sum256([]byte(password + hex.EncodeToString(salt)))

	// Prepare SQL statement with placeholders to prevent SQL injection
	stmt, err := db.Prepare("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	// Execute the query safely
	res, err := stmt.Exec(username, hex.EncodeToString(hashedPassword[:]), hex.EncodeToString(salt))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Check if insertion was successful
	rowsAffected, err := res.RowsAffected()
	if err != nil || rowsAffected == 0 {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "User registered successfully")
}

func sanitiseInput(input string) string {
	input = fmt.Sprintf("%s", input)
	input = regexp.MustCompile(`^\s+|\s+$`).ReplaceAllString(input, "")
	input = regexp.MustCompile(`[^\w.-]`).ReplaceAllString(input, "")
	return input
}

func isValidUsername(username string) bool {
	if len(username) < 3 || len(username) > 256 {
		return false
	}
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9.-_]+$`, username)
	return matched
}

func isValidPassword(password string) bool {
	if len(password) < 8 {
		return false
	}
	matched, _ := regexp.MatchString(`^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$`, password)
	return matched
}
