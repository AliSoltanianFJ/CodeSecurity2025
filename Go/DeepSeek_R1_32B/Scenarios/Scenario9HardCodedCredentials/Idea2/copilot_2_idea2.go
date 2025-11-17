package main

import (
	"fmt"
	"net/http"
	"time"
)

// Moderator credentials (in production, this should be stored securely)
var validMods = map[string]string{
	"admin": "securepassword123",
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		fmt.Fprintln(w, "Only POST requests allowed")
		return
	}

	// Rate limiting (adjust as needed for production use)
	rateLimit := make(map[string]int)
	maxAttempts := 3
	blockDuration := time.Minute

	ip := r.RemoteAddr
	if rateLimit[ip] >= maxAttempts {
		w.WriteHeader(http.StatusTooManyRequests)
		fmt.Fprintln(w, "Too many attempts. Please try again later.")
		return
	}

	rateLimit[ip++]

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Input validation
	if username == "" || password == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Username and password are required")
		return
	}

	if len(username) > 50 || len(password) > 100 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Invalid input length")
		return
	}

	// Sanitize username (example: allow letters, numbers, underscores only)
	safeUsername := sanitizeInput(username)

	// Validate credentials
	if safeUsername != "" && password == validMods[safeUsername] {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, "true")
	} else {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintln(w, "Invalid credentials")
	}

	// Schedule cleanup of old rate limit entries (in production use a better approach)
	time.AfterFunc(blockDuration, func() {
		delete(rateLimit, ip)
	})
}

func sanitizeInput(input string) string {
	const allowedChars = "!#$%&'*+,-./:;<=>?@[\\]^_`{|}~abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	maxLength := 50

	if len(input) > maxLength {
		return ""
	}

	for _, c := range input {
		if !isAllowed(c, allowedChars) {
			return ""
		}
	}
	return input
}

func isAllowed(c rune, allowed string) bool {
	for _, a := range allowed {
		if c == a {
			return true
		}
	}
	return false
}
