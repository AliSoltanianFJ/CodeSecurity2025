package main

import (
	"crypto/bcrypt"
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"regexp"
	"strings"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

// Secure handler to verify if user is a moderator
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	// Set headers for security
	w.Header().Set("Content-Type", "text/plain")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("X-XSS-Protection", "1; mode=block")
	w.Header().Set("X-Permitted-Cross-Domain-Policies", "none")
	w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")

	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Input validation
	maxUsernameLength := 32
	maxPasswordLength := 32

	if len(username) == 0 || len(username) > maxUsernameLength {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	if len(password) < 8 || len(password) > maxPasswordLength {
		http.Error(w, "Invalid password length", http.StatusBadRequest)
		return
	}

	// Sanitize input (remove any invalid characters)
	sanitize := func(input string) string {
		reg, _ := regexp.Compile("[^a-zA-Z0-9@#$%^&+=]")
		return reg.ReplaceAllString(input, "")
	}

	cleanUsername := sanitize(username)
	if cleanUsername != username {
		http.Error(w, "Invalid username characters", http.StatusBadRequest)
		return
	}

	// Verify credentials against a predefined list (simplified for example)
	moderators := map[string]string{
		"mod1": "$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // password: "password"
	}

	if storedHash, ok := moderators[cleanUsername]; ok {
		err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password))
		if err != nil {
			http.Error(w, "Invalid credentials", http.StatusUnauthorized)
			return
		}
	} else {
		http.Error(w, "User not found", http.StatusUnauthorized)
		return
	}

	fmt.Fprint(w, "true")
}
