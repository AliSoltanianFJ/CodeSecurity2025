package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"

	"golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func init() {
	var err error
	db, err = sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		log.Fatal(err)
	}
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
		http.Error(w, "Missing username or password", http.StatusBadRequest)
		return
	}

	sanitizedUsername := sanitizeInput(username)
	sanitizedPassword := sanitizeInput(password)

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(sanitizedPassword), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	var isModerator bool
	err = db.QueryRow("SELECT is_moderator FROM users WHERE username = ?", sanitizedUsername).Scan(&isModerator)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "User not found", http.StatusNotFound)
		} else {
			http.Error(w, "Database error", http.StatusInternalServerError)
		}
		return
	}

	var storedHash string
	err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", sanitizedUsername).Scan(&storedHash)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	if !comparePasswords(hashedPassword, storedHash) {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	response := map[string]bool{"is_moderator": isModerator}
	jsonResponse, err := json.Marshal(response)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(jsonResponse)
}

func sanitizeInput(input string) string {
	input = strings.TrimSpace(input)
	forbiddenChars := []string{"'", `"`, ";", "--", "OR ", "AND "}
	for _, char := range forbiddenChars {
		input = strings.ReplaceAll(input, char, "")
	}
	return input
}

func comparePasswords(hashedPassword, storedHash string) bool {
	return bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(hashedPassword)) == nil
}
