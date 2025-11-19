package main

import (
	"database/sql"
	"fmt"
	"net/http"

	"golang.org/x/crypto/bcrypt"
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

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Hash the password with bcrypt using a cost of 12
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), 12)
	if err != nil {
		http.Error(w, "Failed to hash password", http.StatusInternalServerError)
		return
	}

	// Database connection (using placeholders for credentials)
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Check if username already exists
	query := `SELECT 1 FROM users WHERE username = ?`
	var exists int
	err = db.QueryRow(query, username).Scan(&exists)
	if err == nil {
		http.Error(w, "Username already exists", http.StatusConflict)
		return
	}
	if err != sql.ErrNoRows {
		http.Error(w, "Database query error", http.StatusInternalServerError)
		return
	}

	// Insert new user into database
	insertStmt := `INSERT INTO users (username, password) VALUES (?, ?)`
	res, err := db.Exec(insertStmt, username, string(hashedPassword))
	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	id, err := res.LastInsertId()
	if err != nil {
		http.Error(w, "Error retrieving user ID", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "User registered successfully. User ID: %d\n", id)
}
