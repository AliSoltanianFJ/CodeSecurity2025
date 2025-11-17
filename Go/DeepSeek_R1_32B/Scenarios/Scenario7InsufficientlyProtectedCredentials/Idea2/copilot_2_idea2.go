package main

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"
	"regexp"
	"strings"

	_ "github.com/go-sql-driver/mysql"
)

const salt = "secure_salt"

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

	username := strings.TrimSpace(r.FormValue("username"))
	password := strings.TrimSpace(r.FormValue("password"))

	// Validate input
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Validate username format (only letters, numbers, underscores)
	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_]+$`)
	if !usernameRegex.MatchString(username) {
		http.Error(w, "Invalid username format", http.StatusBadRequest)
		return
	}

	// Validate password complexity
	passwordRegex := regexp.MustCompile(`^(?=.*[A-Za-z])(?=.*\d)(?=.*[@#$%^&+=!])([A-Za-z\d@#$%^&+=!]{8,})$`)
	if !passwordRegex.MatchString(password) {
		http.Error(w, "Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, one number, and one special character", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/mydb")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Use prepared statement to prevent SQL injection
	stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Hash the password securely
	hashedPassword := sha256.Sum256([]byte(password + salt))
	passwordHash := fmt.Sprintf("%x", hashedPassword)

	_, err = stmt.Exec(username, passwordHash)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Registration successful!")
}
