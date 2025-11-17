package main

import (
	"fmt"
	"net/http"
	"regexp"
	"strings"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	err := r.ParseForm()
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error parsing form: %v", err)
		return
	}

	username := strings.TrimSpace(r.Form.Get("username"))
	password := strings.TrimSpace(r.Form.Get("password"))

	if username == "" || password == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Username and password are required")
		return
	}

	sanitizedUsername, err := sanitizeInput(username)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid username: %v", err)
		return
	}

	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9._-]+$`)
	passwordRegex := regexp.MustCompile(`^.{8,128}$`)

	if !usernameRegex.MatchString(sanitizedUsername) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Invalid username format")
		return
	}

	if !passwordRegex.MatchString(password) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Invalid password format")
		return
	}

	isModerator := checkModeratorCredentials(sanitizedUsername, password)

	if isModerator {
		w.WriteHeader(http.StatusOK)
		fmt.Fprint(w, "true")
	} else {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprint(w, "false")
	}
}

func sanitizeInput(input string) (string, error) {
	// Remove any leading/trailing whitespace
	input = strings.TrimSpace(input)

	// Use regex to remove any invalid characters
	validChars := regexp.MustCompile(`[^a-zA-Z0-9._-]`)
	cleaned := validChars.ReplaceAllString(input, "")

	if cleaned == "" {
		return "", fmt.Errorf("empty or invalid input")
	}

	return cleaned, nil
}

func checkModeratorCredentials(username string, password string) bool {
	// In a real application, this would query a secure database
	// and use secure hashing for passwords
	// This is just an example using hardcoded values

	modUsername := "admin"
	modPassword := "securepassword123"

	return username == modUsername && password == modPassword
}
