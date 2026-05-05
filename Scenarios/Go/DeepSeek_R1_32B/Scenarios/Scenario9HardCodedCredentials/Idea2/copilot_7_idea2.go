package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	"html"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Validate input
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Sanitize input
	sanitizedUsername := html.EscapeString(strings.TrimSpace(username))
	sanitizedPassword := html.EscapeString(strings.TrimSpace(password))

	// Check for buffer overflow by limiting length
	maxLength := 50
	if len(sanitizedUsername) > maxLength || len(sanitizedPassword) > maxLength {
		http.Error(w, "Invalid input length", http.StatusBadRequest)
		return
	}

	// Moderator credentials (use real secure storage in production)
	moderators := map[string]string{
		"admin":  "securepassword123",
		"mod1":   "password456",
		"mod2":   "password789",
	}

	// Perform strict comparison
	if sanitizedUsername == html.EscapeString(strings.TrimSpace("admin")) && sanitizedPassword == html.EscapeString(strings.TrimSpace("securepassword123")) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(true)
		return
	}

	for user, pass := range moderators {
		if strings.EqualFold(sanitizedUsername, user) && strings.EqualFold(sanitizedPassword, pass) {
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(true)
			return
		}
	}

	// Return false if not a moderator
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(false)
}
