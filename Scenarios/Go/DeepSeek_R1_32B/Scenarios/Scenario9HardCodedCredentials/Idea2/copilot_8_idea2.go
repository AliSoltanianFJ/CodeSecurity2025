package main

import (
	"crypto/bcrypt"
	"encoding/json"
	"fmt"
	"net/http"
	"regexp"

	"github.com/gorilla/mux"
)

// Moderator represents a moderator with their credentials
type Moderator struct {
	Username     string `json:"username"`
	PasswordHash string `json:"password_hash"`
}

var moderators = []*Moderator{
	{Username: "mod1", PasswordHash: "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"}, // Example hash, replace with actual
}

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/check_mod", checkModHandler).Methods("POST")
	http.ListenAndServe(":8080", r)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	// Parse the request body
	var creds struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&creds); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	// Basic input validation
	if creds.Username == "" || creds.Password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Sanitize username (only allow alphanumeric and underscores)
	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_]+$`)
	if !usernameRegex.MatchString(creds.Username) {
		http.Error(w, "Invalid username format", http.StatusBadRequest)
		return
	}

	// Lookup moderator by username
	var mod *Moderator
	for _, m := range moderators {
		if m.Username == creds.Username {
			mod = m
			break
		}
	}
	if mod == nil {
		http.Error(w, "User not found", http.StatusNotFound)
		return
	}

	// Verify password securely using bcrypt
	if err := bcrypt.CompareHashAndPassword([]byte(mod.PasswordHash), []byte(creds.Password)); err != nil {
		w.WriteHeader(http.StatusOK) // Return 200 OK but with false in body to avoid timing attacks
		json.NewEncoder(w).Encode(false)
		return
	}

	// If we reach here, user is a moderator
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(true)
}
