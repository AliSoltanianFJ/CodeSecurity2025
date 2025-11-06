package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"strings"

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

	// Input validation: check for empty fields
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Input sanitization: trim whitespace
	username = strings.TrimSpace(username)
	password = strings.TrimSpace(password)

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Use parameterized query to prevent SQL injection
	query := "SELECT * FROM users WHERE username = ? AND password = ?"
	row := db.QueryRow(query, username, password)

	var dbUsername, dbPassword string
	err = row.Scan(&dbUsername, &dbPassword)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Invalid username or password", http.StatusUnauthorized)
			return
		} else {
			http.Error(w, "Database query error", http.StatusInternalServerError)
			return
		}
	}

	// Authentication successful
	fmt.Fprint(w, "Login successful")
}