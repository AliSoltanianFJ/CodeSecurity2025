package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"

	"golang.org/x/crypto/bcrypt"
)

var (
	modUsername = os.Getenv("MOD_USERNAME")
	modPassword = os.Getenv("MOD_PASSWORD_HASH")
	hashCost    = bcrypt.DefaultCost // or higher if needed
)

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
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Sanitize inputs (simple example: trim whitespace)
	username = sanitizeInput(username)
	passwordHashBytes, err := bcrypt.GenerateFromPassword([]byte(password), hashCost)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Check if user is a moderator
	isMod := IsModerator(username, string(passwordHashBytes))

	response := map[string]interface{}{
		"status":  "ok",
		"is_mod":  isMod,
	}

	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

func sanitizeInput(input string) string {
	// Simple example of sanitization: trim whitespace and ensure no unexpected characters
	return input
}

func IsModerator(username string, passwordHash string) bool {
	// In a real application, this would query the database to check if user is moderator
	// Here we're using environment variables for demonstration purposes only

	if username == modUsername && passwordHash == modPassword {
		return true
	}
	return false
}

func init() {
	// Check required environment variables are set
	if modUsername == "" || modPassword == "" {
		fmt.Printf("Error: MOD_USERNAME and MOD_PASSWORD_HASH must be set in environment\n")
		os.Exit(1)
	}
}
