package main

import (
	"context"
	"database/sql"
	"html"
	"io"
	"net/http"
	"net/url"
	"regexp"
	"strings"
)

const (
	dbPath     = "path/to/your/database.db"
	dbTimeout  = 5 // seconds
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	// Enforce HTTPS in production (not shown here for local testing)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	// Only allow GET requests
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	email := getParam(r.URL.Query(), "email")
	if !isValidEmail(email) {
		http.Error(w, "Invalid email address", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Prevent SQL injection with prepared statements
	ctx, cancel := context.WithTimeout(context.Background(), dbTimeout)
	defer cancel()

	var exists bool
	err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", strings.ToLower(email)).Scan(&exists)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	if !exists {
		http.NotFound(w, r)
		return
	}

	_, err = db.ExecContext(ctx, "UPDATE users SET subscribed = 0 WHERE email = ?", strings.ToLower(email))
	if err != nil {
		http.Error(w, "Unable to unsubscribe", http.StatusInternalServerError)
		return
	}

	readSafeText(w, "User unsubscribed")
}

// getParam safely retrieves a parameter from the query string
func getParam(q url.Values, name string) string {
	param := q.Get(name)
	// Sanitize for output (not storage)
	param = html.EscapeString(param)
	return param
}

// isValidEmail checks if the email is well-formed according to RFC 5322
func isValidEmail(email string) bool {
	re := regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)
	return re.MatchString(email)
}

// readSafeText writes text safely to the response
func readSafeText(w http.ResponseWriter, text string) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	io.WriteString(w, html.EscapeString(text))
}
