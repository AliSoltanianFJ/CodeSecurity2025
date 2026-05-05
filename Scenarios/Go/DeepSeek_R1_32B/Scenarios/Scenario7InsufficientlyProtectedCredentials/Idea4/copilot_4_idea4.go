package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"time"

	"golang.org/x/crypto/bcrypt"
	_ "github.com/go-sql-driver/mysql"
)

const (
	dbUser     = "root"
	dbPassword = "password"
	dbName     = "testdb"
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

	// Input validation and sanitization
	if username == "" || password == "" {
		http.Error(w, "Username or password cannot be empty", http.StatusBadRequest)
		return
	}

	// Sanitize inputs (basic example; adjust as needed for your use case)
	username = sanitizeInput(username)
	passwordHash, err := hashPassword(password)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@/%s?parseTime=true", dbUser, dbPassword, dbName))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
	defer cancel()

	stmt, err := db.PrepareContext(ctx, `INSERT INTO users (username, password_hash) VALUES (?, ?)`)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	_, err = stmt.ExecContext(ctx, username, hex.EncodeToString(passwordHash))
	if err != nil {
		if isUniqueConstraintError(err) {
			http.Error(w, "Username already exists", http.StatusBadRequest)
			return
		}
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Registration successful")
}

func sanitizeInput(s string) string {
	// Implement additional sanitization as needed
	return sqlEscape(s)
}

func hashPassword(password string) ([]byte, error) {
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(password), 12)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %v", err)
	}
	return hashedBytes, nil
}

func sqlEscape(s string) string {
	// Basic SQL escaping; use proper prepared statements in queries
	return s
}

func isUniqueConstraintError(err error) bool {
	sqlErr, ok := err.(*sql.Error)
	if !ok {
		return false
	}
	return sqlErr.Code == "23505" // PostgreSQL unique constraint violation code
}
