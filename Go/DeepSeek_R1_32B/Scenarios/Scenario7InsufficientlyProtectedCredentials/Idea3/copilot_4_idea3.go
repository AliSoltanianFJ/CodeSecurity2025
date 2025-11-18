package main

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/register", registerHandler)
	http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")
	/* hash the password */
	if username == "" || password == "" {
		http.Error(w, "Username and password required", http.StatusBadRequest)
		return
	}

	// Sanitize username for output (never echo password)
	safeUsername := html.EscapeString(username)

	// Basic validation: ensure no spaces, at least 3 chars
	if strings.Contains(username, " ") || len(username) < 3 {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	if strings.Contains(password, " ") || len(password) < 3 {
		http.Error(w, "Invalid password", http.StatusBadRequest)
		return
	}

	// Securely hash the password using bcrypt with default cost
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Connect to database (update DSN as appropriate)
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		http.Error(w, "Database connection error", http.StatusServiceUnavailable)
		return
	}
	defer db.Close()

	// Check if username exists (using parameterized query)
	var exists bool
	err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", safeUsername).Scan(&exists)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	if exists {
		http.Error(w, "Username already taken", http.StatusConflict)
		return
	}

	// Insert new user (using parameterized query)
	_, err = db.Exec("INSERT INTO users (username, password) VALUES (?, ?)", safeUsername, hashedPassword)
	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "User registered successfully: %s", safeUsername)
}
