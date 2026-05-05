package main

import (
	"database/sql"
	"fmt"
	"log"
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

	// Only allow application/x-www-form-urlencoded content type
	contentType := r.Header.Get("Content-Type")
	if !strings.HasPrefix(contentType, "application/x-www-form-urlencoded") {
		http.Error(w, "Invalid Content-Type", http.StatusBadRequest)
		return
	}

	// Parse form with size limit
	r.ParseForm(1024)

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Validate input: allow only 3-32 chars and alphanumeric + underscore
	if !isValidCredentials(username, password) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Invalid username or password.")
		return
	}

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		log.Printf("DB open error: %v", err)
		return
	}
	defer db.Close()

	var hashedPassword string
	err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&hashedPassword)
	if err == sql.ErrNoRows {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprint(w, "Invalid credentials.")
		return
	} else if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		log.Printf("DB query error: %v", err)
		return
	}

	// Compare passwords (assumes password is stored as hash)
	if password != hashedPassword {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprint(w, "Invalid credentials.")
		return
	}

	fmt.Fprintf(w, "Login successful!")
}

// isValidCredentials validates username and password are 3-32 chars and contain only allowed characters.
func isValidCredentials(username, password string) bool {
	if len(username) < 3 || len(username) > 32 || len(password) < 3 || len(password) > 32 {
		return false
	}
	for _, c := range username + password {
		if !(c >= 'a' && c <= 'z') &&
			!(c >= 'A' && c <= 'Z') &&
			!(c >= '0' && c <= '9') &&
			c != '_' {
			return false
		}
	}
	return true
}
