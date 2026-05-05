package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"os"
	"regexp"

	"github.com/golang-crypto/bcrypt"
	_ "github.com/go-sql-driver/mysql"
)

const (
	maxUsernameLength = 50
	minPasswordLength = 8
	maxPasswordLength = 50
	saltSize          = 16
	hashIterations    = 12
)

var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_.]+$`)

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

	// Validate input lengths
	if len(username) == 0 || len(password) == 0 {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if len(username) > maxUsernameLength {
		http.Error(w, "Username too long", http.StatusBadRequest)
		return
	}

	if len(password) < minPasswordLength || len(password) > maxPasswordLength {
		http.Error(w, fmt.Sprintf("Password must be between %d and %d characters", minPasswordLength, maxPasswordLength), http.StatusBadRequest)
		return
	}

	// Sanitize username
	sanitizedUsername := sanitizeUsername(username)
	if sanitizedUsername == "" {
		http.Error(w, "Invalid username format", http.StatusBadRequest)
		return
	}

	// Validate password complexity (can be enhanced further)
	if !isPasswordComplex(password) {
		http.Error(w, "Weak password. Must contain letters and numbers.", http.StatusBadRequest)
		return
	}

	// Hash the password with bcrypt
	hashedPassword, err := hashPassword([]byte(password))
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Database connection
	db, err := sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Check if username already exists
	exists, err := checkUsernameExists(db, sanitizedUsername)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	if exists {
		http.Error(w, "Username already taken", http.StatusConflict)
		return
	}

	// Insert new user into database
	if _, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", sanitizedUsername, hex.EncodeToString(hashedPassword)); err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Registration successful")
}

func sanitizeUsername(username string) string {
	return usernameRegex.ReplaceAllString(username, "")
}

func isPasswordComplex(password string) bool {
	if len(password) < minPasswordLength {
		return false
	}
	// Add more complex checks as needed (e.g., check for special characters)
	return hasLettersAndNumbers(password)
}

func hasLettersAndNumbers(s string) bool {
	hasLetter := regexp.MustCompile(`[a-zA-Z]`).MatchString(s)
	hasNumber := regexp.MustCompile(`\d`).MatchString(s)
	return hasLetter && hasNumber
}

func hashPassword(password []byte) ([]byte, error) {
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(sha256.Sum256(password[:])), hashIterations)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %v", err)
	}
	return hashedBytes, nil
}

func checkUsernameExists(db *sql.DB, username string) (bool, error) {
	var exists bool
	err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", username).Scan(&exists)
	if err != nil {
		return false, fmt.Errorf("failed to check username existence: %v", err)
	}
	return exists, nil
}
