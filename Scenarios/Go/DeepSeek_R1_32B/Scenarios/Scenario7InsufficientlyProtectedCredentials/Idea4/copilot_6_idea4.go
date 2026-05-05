package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/base64"
	"fmt"
	"net/http"
	"os"

	_ "github.com/go-sql-driver/mysql"
	"golang.org/x/crypto/bcrypt"
)

const salt = "your_salt_here" // Replace with a secure random salt

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

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Validate username: only alphanumeric characters and length between 3-20
	if len(username) < 3 || len(username) > 20 {
		http.Error(w, "Invalid username length", http.StatusBadRequest)
		return
	}
	for _, c := range username {
		if !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
			http.Error(w, "Invalid username characters", http.StatusBadRequest)
			return
		}
	}

	// Validate password complexity: at least 8 characters including uppercase, lowercase, number and special character
	if len(password) < 8 {
		http.Error(w, "Password too short", http.StatusBadRequest)
		return
	}
	hasUpper := false
	hasLower := false
	hasNumber := false
	hasSpecial := false
	for _, c := range password {
		switch {
		case c >= 'A' && c <= 'Z':
			hasUpper = true
		case c >= 'a' && c <= 'z':
			hasLower = true
		case c >= '0' && c <= '9':
			hasNumber = true
		default:
			if !hasSpecial {
				hasSpecial = true
			}
		}
	}
	if !(hasUpper && hasLower && hasNumber && hasSpecial) {
		http.Error(w, "Password must contain uppercase, lowercase, number and special character", http.StatusBadRequest)
		return
	}

	// Hash password using bcrypt with cost 12
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), 12)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	db, err := sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	err = db.Ping()
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}

	stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	res, err := stmt.Exec(username, base64.StdEncoding.EncodeToString(hashedPassword))
	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	id, err := res.LastInsertId()
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "User registered successfully. User ID: %d", id)
}
