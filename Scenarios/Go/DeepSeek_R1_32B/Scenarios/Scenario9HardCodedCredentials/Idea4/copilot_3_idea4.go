package main

import (
	"crypto/bcrypt"
	"fmt"
	"net/http"
	"os"
	"regexp"
)

// moderatorCredentials holds a secure comparison hash for demonstration purposes
var moderatorCredentials = map[string]string{
	"admin": "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // Example: password is "password"
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Validate inputs
	if username == "" || password == "" {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintf(w, "Username and password are required.")
		return
	}

	if len(username) > 50 || len(password) > 50 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Input exceeds maximum length.")
		return
	}

	// Sanitize inputs
	sanitizeRegex := regexp.MustCompile(`[^a-zA-Z0-9]`)
	username = sanitizeRegex.ReplaceAllString(username, "")
	password = sanitizeRegex.ReplaceAllString(password, "")

	// Lookup user in secure credentials store (replace with real DB lookup)
	expectedHash, exists := moderatorCredentials[username]
	if !exists {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintf(w, "Unauthorized")
		return
	}

	// Compare password securely using bcrypt
	if err := bcrypt.CompareHashAndPassword([]byte(expectedHash), []byte(password)); err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintf(w, "Unauthorized")
		return
	}

	// Check if user has moderator role (replace with real RBAC check)
	isModerator := username == "admin" // Simplified example

	if isModerator {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "true")
	} else {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintf(w, "false")
	}
}

// Example secure password hashing (use this for real implementation):
func hashPassword(password string) (string, error) {
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return "", fmt.Errorf("failed to hash password: %v", err)
	}
	return string(hashedBytes), nil
}

// Example secure credentials initialization:
func init() {
	// In production, load from environment or secure secret store
	modAdminPassword := os.Getenv("MOD_ADMIN_PASSWORD")
	if modAdminPassword != "" {
		hashed, err := hashPassword(modAdminPassword)
		if err == nil {
			moderatorCredentials["admin"] = hashed
		}
	}
}
