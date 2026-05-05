package main

import (
	"context"
	"database/sql"
	"net/http"
	"os"
	"html/template"
	"regexp"
	"time"
	"log"

	_ "github.com/go-sql-driver/mysql"
)

var dbTimeout = 5 * time.Second

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	params := r.URL.Query()
	email := params.Get("email")

	// Basic email validation (not RFC 5322 compliant)
	if !isValidEmail(email) {
		http.Error(w, "Invalid email address", http.StatusBadRequest)
		return
	}

	// Sanitize email for output (HTML safe)
	safeEmail := html.EscapeString(email)

	// Database operations with context
	ctx, cancel := context.WithTimeout(context.Background(), dbTimeout)
	defer cancel()

	// Open database connection (use environment variables for credentials)
	db, err := sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	var exists bool
	err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
	if err != nil {
		http.Error(w, "Database query failed", http.StatusInternalServerError)
		return
	}

	if !exists {
		// Use templates for consistent output (no XSS here due to prior sanitization)
		t := template.Must(template.New("notFound").Parse(`Email not found: {{ .email }}`))
		t.Execute(w, safeEmail)
		return
	}

	// Unsubscribe by updating a flag rather than deleting records
	_, err = db.ExecContext(ctx, "UPDATE users SET active = 0 WHERE email = ?", email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	t := template.Must(template.New("success").Parse(`Successfully unsubscribed: {{ .email }}`))
	t.Execute(w, safeEmail)
}

// Simple email validation (not RFC 5322 compliant)
func isValidEmail(email string) bool {
	pattern := `(?i)^[^@]+@[^@]+$`
	return regexp.MustCompile(pattern).MatchString(email)
}
