package main

import (
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

const (
	dbUser     = "your_db_user"
	dbPassword = "your_db_password"
	dbName     = "your_db_name"
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

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Sanitize username input to prevent SQL injection or other attacks
	escapedUsername, err := sanitizeInput(username)
	if err != nil {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	// Generate salt for password hashing
	salt := make([]byte, 32)
	if _, err := rand.Read(salt); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Hash the password with the generated salt
	hashedPassword := hashPassword(password, salt)

	// Establish database connection
	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@/%s?parseTime=true", dbUser, dbPassword, dbName))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Check if username already exists
	var existingUsername string
	err = db.QueryRow("SELECT username FROM users WHERE username=?", escapedUsername).Scan(&existingUsername)
	if err == sql.ErrNoRows {
		// Insert new user into database
		stmt, err := db.Prepare("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")
		if err != nil {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		_, err = stmt.Exec(escapedUsername, hex.EncodeToString(hashedPassword), hex.EncodeToString(salt))
		if err != nil {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	} else if err == nil {
		http.Error(w, "Username already exists", http.StatusConflict)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Registration successful")
}

func sanitizeInput(input string) (string, error) {
	// Escape special characters for SQL queries
	return sql.EscapeIdentifier(input)
}

func hashPassword(password string, salt []byte) []byte {
	passwordBytes := []byte(password)
	hash := sha256.New()
	hash.Write(salt)
	hash.Write(passwordBytes)
	return hash.Sum(nil)
}
