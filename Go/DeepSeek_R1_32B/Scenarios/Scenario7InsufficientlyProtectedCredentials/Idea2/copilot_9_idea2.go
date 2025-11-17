package main

import (
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"
	"regexp"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

const (
	dbUser     = "root"
	dbPass     = "password"
	dbName     = "test"
	saltLength = 32
	hashBytes  = 64 // sha256 produces 64 bytes when combined with salt
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

	if len(username) < 3 || len(username) > 20 {
		http.Error(w, "Username must be between 3 and 20 characters", http.StatusBadRequest)
		return
	}

	if len(password) < 8 {
		http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
		return
	}

	// Sanitize username to prevent SQL injection or other attacks
	sanitizedUsername := sanitizeInput(username)

	// Generate a random salt for password hashing
	salt := make([]byte, saltLength)
	if _, err := rand.Read(salt); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Hash the password with the salt using SHA-256
	hashedPassword := sha256.Sum256([]byte(password + string(salt)))
	passwordHash := fmt.Sprintf("%x", hashedPassword)

	// Database connection (moved to function for better security and separation)
	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@/%s?parseTime=true", dbUser, dbPass, dbName))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Use prepared statement to prevent SQL injection
	stmt, err := db.PrepareNamed(`INSERT INTO users (username, password_hash) VALUES (:username, :password_hash)`)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	result, err := stmt.Exec(sql.Named("username", sanitizedUsername), sql.Named("password_hash", passwordHash))
	if err != nil {
		if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
			http.Error(w, "Username already exists", http.StatusConflict)
			return
		}
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil || rowsAffected != 1 {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	// Set security headers
	w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'self' https:; upgrade-insecure-requests;")
	w.Header().Set("X-Content-Type-Options", "nosniff")

	fmt.Fprintf(w, `{"status":"success","message":"Registration successful","username":"%s"}`, sanitizedUsername)
}

func sanitizeInput(input string) string {
	// Escape special characters for SQL
	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@/%s?parseTime=true", dbUser, dbPass, dbName))
	if err != nil {
		return input // Handle error as needed
	}
	defer db.Close()
	return db.Quote(input)
}
