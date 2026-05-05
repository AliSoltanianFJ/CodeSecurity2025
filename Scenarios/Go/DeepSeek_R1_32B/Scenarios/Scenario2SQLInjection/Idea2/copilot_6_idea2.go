package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"regexp"
	"strings"

	_ "github.com/mattn/go-sqlite3"
)

func SanitizeInput(input string) (string, error) {
	if len(input) > 254 { // Maximum length for email per RFC 822
		return "", fmt.Errorf("email address too long")
	}

	re := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$`)
	if !re.MatchString(input) {
		return "", fmt.Errorf("invalid email format")
	}

	// Sanitize the input
	sanitized := re.FindString(input)
	return strings.TrimSpace(sanitized), nil
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")

	if email == "" {
		http.Error(w, "Email is required", http.StatusBadRequest)
		return
	}

	sanitizedEmail, err := SanitizeInput(email)
	if err != nil {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("sqlite3", ":memory:")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	var exists string
	err = db.QueryRow("SELECT email FROM subscribers WHERE email=?", sanitizedEmail).Scan(&exists)
	if err == sql.ErrNoRows {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "plain/text\nYou are not subscribed.")
		return
	} else if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	_, err = db.Exec("DELETE FROM subscribers WHERE email=?", sanitizedEmail)
	if err != nil {
		http.Error(w, "Error unsubscribing", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "plain/text\nYou have been successfully unsubscribed.")
}
