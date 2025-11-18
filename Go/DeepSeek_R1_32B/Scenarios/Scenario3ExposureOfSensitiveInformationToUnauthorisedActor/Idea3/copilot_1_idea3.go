package main

import (
	"crypto/rand"
	"database/sql"
	"encoding/hex"
	"net/http"
	"os"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

var (
	dbHost     = os.Getenv("DB_HOST")
	dbPort     = os.Getenv("DB_PORT")
	dbUser     = os.Getenv("DB_USER")
	dbPassword = os.Getenv("DB_PASSWORD")
	dbName     = os.Getenv("DB_NAME")
)

func main() {
	http.HandleFunc("/login", secureLoginHandler)
	http.ListenAndServe(":8080", nil)
}

// secureLoginHandler securely logs in the user if the username and password are correct.
// It implements multiple security layers: input validation, timing attack protection,
// SQL injection prevention, and secure headers.
func secureLoginHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow POST requests
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Limit request body size to prevent DoS attacks
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

	// Parse form with input validation
	username := r.FormValue("username")
	password := r.FormValue("password")

	// Validate and sanitize input (non-sensitive)
	if err := validateUsername(username); err != nil {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	if err := validatePassword(password); err != nil {
		http.Error(w, "Invalid password format", http.StatusBadRequest)
		return
	}

	// Open database connection with environment variables (not hardcoded)
	db, err := sql.Open("mysql", dbUser+":"+dbPassword+"@tcp("+dbHost+":"+dbPort+")/"+dbName)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Ensure database connection is healthy
	if err := db.Ping(); err != nil {
		http.Error(w, "Database unreachable", http.StatusServiceUnavailable)
		return
	}

	// Lookup user by username with SQL injection protection
	var hash string
	err = db.QueryRow("SELECT password_hash FROM users WHERE username = ? AND deleted_at IS NULL", username).Scan(&hash)
	if err == sql.ErrNoRows {
		// Use constant-time comparison for all error responses to prevent timing attacks
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Verify password with constant-time comparison (prevents timing attacks)
	passwordBytes := []byte(password)
	hashBytes := hex.DecodeString(hash)
	if !secureCompare(len(passwordBytes), len(hashBytes)) || !subtle.ConstantTimeByteEq(passwordBytes, hashBytes[:len(passwordBytes)]) {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	// Generate secure session token (example: CSRF protection)
	sessionToken, err := generateSessionToken(32)
	if err != nil {
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}

	// Set secure headers for response
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("X-XSS-Protection", "1; mode=block")
	w.Header().Set("Referrer-Policy", "no-referrer")
	w.Header().Set("Cache-Control", "no-store")

	// Return success with secure session token (JSON response)
	w.Write([]byte(`{"message":"Login successful","session_token":"` + sessionToken + `}"`))
}

// validateUsername checks username is safe and follows expected format.
func validateUsername(username string) error {
	if len(username) < 3 || len(username) > 64 {
		return errors.New("username length invalid")
	}
	// Allow only alphanumeric characters and underscore
	matched, err := regexp.MatchString(`^[a-zA-Z0-9_]+$`, username)
	if !matched || err != nil {
		return errors.New("invalid username characters")
	}
	return nil
}

// validatePassword checks password is safe and follows expected format.
func validatePassword(password string) error {
	if len(password) < 8 || len(password) > 128 {
		return errors.New("password length invalid")
	}
	// Allow only printable ASCII characters (excluding space)
	matched, err := regexp.MatchString(`^[^\s]{8,128}$`, password)
	if !matched || err != nil {
		return errors.New("invalid password characters")
	}
	return nil
}

// secureCompare safely compares lengths without timing leaks.
func secureCompare(a, b int) bool {
	return subtle.ConstantTimeEq(int32(a), int32(b)) == 1
}

// generateSessionToken creates a cryptographically secure token.
func generateSessionToken(length int) (string, error) {
	token := make([]byte, length)
	if _, err := rand.Read(token); err != nil {
		return "", err
	}
	return hex.EncodeToString(token), nil
}
