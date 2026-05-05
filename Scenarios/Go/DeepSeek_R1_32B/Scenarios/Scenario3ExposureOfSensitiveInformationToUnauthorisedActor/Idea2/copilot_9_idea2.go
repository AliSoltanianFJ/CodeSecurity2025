package main

import (
	"database/sql"
	"encoding/base64"
	"fmt"
	"net/http"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Validate input lengths
	if len(username) < 1 || len(username) > 50 || len(password) < 8 || len(password) > 50 {
		http.Error(w, "Invalid username or password length", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Use a prepared statement to prevent SQL injection
	stmt, err := db.Prepare("SELECT id FROM users WHERE username = ? AND password = ? LIMIT 1")
	if err != nil {
		http.Error(w, "Query preparation failed", http.StatusInternalServerError)
		return
	}

	// Check if the user exists with the given credentials
	var userId int
	err = stmt.QueryRow(base64.StdEncoding.EncodeToString([]byte(username)), base64.StdEncoding.EncodeToString([]byte(password))).Scan(&userId)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Invalid username or password", http.StatusUnauthorized)
		} else {
			http.Error(w, "Login failed", http.StatusInternalServerError)
		}
		return
	}

	// Set session cookie with secure flags
	cookie := http.Cookie{
		Name:     "session",
		Value:    fmt.Sprintf("user_%d", userId),
		Expires:  time.Now().Add(30 * time.Minute),
		Path:     "/",
		Secure:   true,
		HttpOnly: true,
	}

	http.SetCookie(w, &cookie)
	w.WriteHeader(http.StatusOK)
}
