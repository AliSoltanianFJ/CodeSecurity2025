package main

import (
	"crypto"
	"crypto/rand"
	"database/sql"
	"encoding/base64"
	"fmt"
	"net/http"

	"golang.org/x/crypto/pbkdf2"
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

	// Validate input
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Generate a salt
	salt := make([]byte, 32)
	if _, err := rand.Read(salt); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Hash the password with PBKDF2
	const iterations = 10000
	hashedPassword := pbkdf2.Key([]byte(password), salt, iterations, 32, crypto.SHA256)

	// Encode salt and hashed password to base64 for storage
	saltStr := base64.URLEncoding.EncodeToString(salt)
	passwordHash := base64.URLEncoding.EncodeToString(hashedPassword)

	// Database connection
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Check if username exists
	var exists int
	err = db.QueryRow("SELECT 1 FROM users WHERE username = ?", username).Scan(&exists)
	switch {
	case err == sql.ErrNoRows:
		// Proceed to insert new user
		stmt, err := db.Prepare("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")
		if err != nil {
			http.Error(w, "Database error", http.StatusInternalServerError)
			return
		}
		defer stmt.Close()

		result, err := stmt.Exec(username, passwordHash, saltStr)
		if err != nil {
			http.Error(w, "Registration failed", http.StatusInternalServerError)
			return
		}

		id, err := result.LastInsertId()
		if err != nil {
			http.Error(w, "Error retrieving user ID", http.StatusInternalServerError)
			return
		}

		fmt.Fprintf(w, "User registered successfully. User ID: %d\n", id)
	case err == nil:
		http.Error(w, "Username already taken", http.StatusConflict)
	default:
		http.Error(w, "Database query error", http.StatusInternalServerError)
	}
}
