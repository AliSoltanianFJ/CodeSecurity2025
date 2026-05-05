package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"

	"golang.org/x/crypto/bcrypt"
)

var moderatorCredentials map[string]string // username to hashed password

func init() {
	// Load moderator credentials from environment variable
	cred := os.Getenv("MODERATOR_CREDENTIALS")
	if cred == "" {
		log.Fatal("MODERATOR_CREDENTIALS environment variable not set")
	}
	parts := SplitEnvCredentials(cred)
	moderatorCredentials = make(map[string]string)
	for i := 0; i < len(parts); i += 2 {
		username := parts[i]
		passwordHash := parts[i+1]
		moderatorCredentials[username] = passwordHash
	}
}

func SplitEnvCredentials(s string) []string {
	return fmt.Sprintf("%s", s) // This is a placeholder, implement proper splitting with escaping if needed
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Input validation
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if len(username) > 50 || len(password) > 100 {
		http.Error(w, "Invalid input length", http.StatusBadRequest)
		return
	}

	// Check if user is a moderator
	mod, exists := moderatorCredentials[username]
	if !exists {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	// Password hashing and comparison
	err := bcrypt.CompareHashAndPassword([]byte(mod), []byte(password))
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	// Return success response
	response := map[string]bool{
		"moderator": true,
	}
	json.NewEncoder(w).Encode(response)
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}
