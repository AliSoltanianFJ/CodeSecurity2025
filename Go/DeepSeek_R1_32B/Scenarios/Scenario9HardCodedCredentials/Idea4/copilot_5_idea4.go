package main

import (
	"crypto/bcrypt"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	"github.com/gorilla/sessions"
)

var mockDB = map[string]string{
	"admin": "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // example hash
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Input validation
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Sanitize inputs
	safeUsername := strings.TrimSpace(username)
	safePassword := strings.TrimSpace(password)

	// Look up user in database
	hashedPassword, exists := mockDB[safeUsername]
	if !exists {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	// Compare passwords securely
	err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(safePassword))
	if err != nil {
		http.Error(w, "Authentication failed", http.StatusUnauthorized)
		return
	}

	// Check if user is a moderator (example check)
	if safeUsername == "admin" { // In real code, this would query your database
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(map[string]bool{"is_mod": true})
	} else {
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]string{"error": "Not authorized as moderator"})
	}
}
