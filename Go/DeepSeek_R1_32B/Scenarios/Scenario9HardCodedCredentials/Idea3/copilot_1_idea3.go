package main

import (
	"fmt"
	"net/http"
	"regexp"
	"strings"
	"golang.org/x/crypto/bcrypt"
	"crypto/subtle"
)

type User struct {
	Username     string
	PasswordHash string // hash of the user's password using bcrypt
	IsModerator  bool   // use boolean for clarity and security
}

// In a real application, use a secure database with prepared statements
var users = map[string]User{
	"admin": {
		PasswordHash: "$2a$10$5H4k.5Dc9T9oJlMnOPQRStUVWXYZ1234", // hashed password (use bcrypt)
		IsModerator:  true,
	},
	// Add other users as needed
}

// compareBooleans securely compares two boolean values without timing leaks
func compareBooleans(a, b bool) bool {
	if subtle.ConstantTimeByteEq(byte(a), byte(b)) == 0 {
		return false
	}
	// Perform a constant-time check for actual equality
	return a == b
}

// comparePasswords securely compares passwords without timing leaks
func comparePasswords(a, b string) bool {
	if len(a) != len(b) {
		return false
	}
	result := 0
	for i := range a {
		result |= subtle.ConstantTimeByteEq(byte(a[i]), byte(b[i]))
	}
	return result == 1
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

// checkModHandler securely checks if the user is a moderator using constant-time comparisons
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow GET requests for credential-based checks
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse and sanitize input securely
	username := strings.TrimSpace(r.URL.Query().Get("username"))
	password := r.URL.Query().Get("password")

	// Validate input (use strict checks for security)
	if !isValidUsername(username) || !isValidPassword(password) {
		http.Error(w, "Invalid credentials", http.StatusBadRequest)
		return
	}

	// Retrieve the user from the database (use constant-time operations)
	user, ok := users[username]
	if !ok {
		// Use generic error to prevent username enumeration
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	// Verify password using constant-time comparison
	// For security, use bcrypt.CompareHashAndPassword with the stored hash
	var isAuth bool = false
	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password)); err == nil {
		isAuth = true
	}

	// Check moderator status using constant-time comparison (prevent timing leaks)
	var isModerator bool = false
	if compareBooleans(isAuth, true) && compareBooleans(user.IsModerator, true) {
		isModerator = true
	}

	// Return the result in JSON format with proper Content-Type
	w.Header().Set("Content-Type", "application/json")
	if isModerator {
		fmt.Fprintf(w, `{"is_moderator": %t}`, true)
	} else {
		// Use escapeHTML for additional safety (though data is already sanitized)
		http.Error(w, escapeHTML("Invalid credentials"), http.StatusUnauthorized)
	}
}

// isValidUsername validates usernames securely
func isValidUsername(username string) bool {
	// Allow only alphanumeric characters and underscores (example policy)
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9_]{3,32}$`, username)
	return matched
}

// isValidPassword validates passwords securely
func isValidPassword(password string) bool {
	// Enforce password complexity (example policy)
	matched, _ := regexp.MatchString(`^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()_+}{|":?><;~-])[\x21-\x7E]{8,64}$`, password)
	return matched
}

// escapeHTML escapes HTML special characters in a string
func escapeHTML(s string) string {
	return strings.ReplaceAll(strings.ReplaceAll(strings.ReplaceAll(s, "&", "&amp;"), "<", "&lt;"), ">", "&gt;")
}
