package main

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"net/http"
	"strings"
	"time"

	"golang.org/x/crypto/bcrypt"
)

var moderatorDB = map[string]string{
	"admin": "secure_password_hash", // Replace with actual secure hash in production
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	if username == "" || password == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Username and password are required.")
		return
	}

	username = trimWhitespace(username)
	password = trimWhitespace(password)

	if len(username) > 32 || len(username) < 3 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid username length.")
		return
	}

	if !isValidUsername(username) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid characters in username.")
		return
	}

	if len(password) > 128 || len(password) < 6 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid password length.")
		return
	}

	hashedPassword := moderatorDB[username]
	if hashedPassword == "" {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintf(w, "Invalid credentials.")
		return
	}

	err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintf(w, "Invalid credentials.")
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     "mod_session",
		Value:    generateSessionToken(),
		Expires:  time.Now().Add(time.Hour),
		HttpOnly: true,
		Secure:   false, // Set to true in production with HTTPS
		SameSite: http.SameSiteLaxMode,
	})

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "true")
}

func trimWhitespace(s string) string {
	return strings.TrimSpace(s)
}

func isValidUsername(username string) bool {
	for _, c := range username {
		if !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
			return false
		}
	}
	return true
}

func generateSessionToken() string {
	randomBytes := make([]byte, 32)
	_, err := rand.Read(randomBytes)
	if err != nil {
		panic(fmt.Sprintf("Failed to generate session token: %v", err))
	}
	return hex.EncodeToString(sha256.Sum256(randomBytes)[:])
}
