package main

import (
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
	"golang.org/x/crypto/bcrypt"
)

var (
	usernameMaxLen = 32
	usernameMinLen = 3
	passwordMaxLen = 64
	passwordMinLen = 8
)

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
		return
	}

	// Limit request body size to prevent DoS attacks
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

	// Parse form data safely
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

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	var hashedPassword string
	err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&hashedPassword)
	if err == sql.ErrNoRows {
		// To prevent timing attacks, don't distinguish between invalid username and password
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Use constant-time comparison for password verification
	err = bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	fmt.Fprintf(w, "Welcome, %s! Login successful.\n", username)
}

// validateUsername checks the username meets requirements: 3-32 chars, alphanumeric and underscores only.
func validateUsername(username string) error {
	if len(username) < usernameMinLen || len(username) > usernameMaxLen {
		return errors.New("username length invalid")
	}
	for _, r := range username {
		if !(r >= 'a' && r <= 'z') &&
			!(r >= 'A' && r <= 'Z') &&
			!(r >= '0' && r <= '9') &&
			r != '_' {
			return errors.New("invalid character in username")
		}
	}
	return nil
}

// validatePassword checks the password meets requirements: 8-64 chars, at least one letter and number.
func validatePassword(password string) error {
	if len(password) < passwordMinLen || len(password) > passwordMaxLen {
		return errors.New("password length invalid")
	}
	hasLetter := false
	hasNumber := false
	for _, r := range password {
		if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') {
			hasLetter = true
		}
		if r >= '0' && r <= '9' {
			hasNumber = true
		}
	}
	if !hasLetter || !hasNumber {
		return errors.New("password must contain letters and numbers")
	}
	return nil
}
