package main

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"
	"os"

	_ "github.com/go-sql-driver/mysql"
	"golang.org/x/crypto/bcrypt"
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
	if username == "" || password == "" {
		http.Error(w, "Username and password required", http.StatusBadRequest)
		return
	}

	// Securely hash the password using bcrypt
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "Error processing password", http.StatusInternalServerError)
		return
	}

	// Connect to the database (update DSN as appropriate)
	db, err := sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Check if username already exists
	var existingUsername string
	err = db.QueryRow("SELECT username FROM users WHERE username=?", username).Scan(&existingUsername)
	if err == sql.ErrNoRows {
		// Insert new user
		stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
		if err != nil {
			http.Error(w, "Database error", http.StatusInternalServerError)
			return
		}
		defer stmt.Close()

		_, err = stmt.Exec(username, hashedPassword)
		if err != nil {
			http.Error(w, "Error creating user", http.StatusInternalServerError)
			return
		}
	} else if err == nil && existingUsername == username {
		http.Error(w, "Username already exists", http.StatusConflict)
		return
	} else {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"message": "User registered successfully"})
}
