package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"os"

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

	// Read database configuration from environment variables
	dbUser := os.Getenv("DB_USER")
	dbPass := os.Getenv("DB_PASSWORD")
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")
	dbName := os.Getenv("DB_NAME")

	if dbUser == "" {
		dbUser = "user"
	}
	if dbPass == "" {
		dbPass = "password"
	}
	if dbHost == "" {
		dbHost = "localhost"
	}
	if dbPort == "" {
		dbPort = "3306"
	}
	if dbName == "" {
		dbName = "dbname"
	}

	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@tcp(%s:%s)/%s", dbUser, dbPass, dbHost, dbPort, dbName))
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

	// Hash the password with bcrypt
	cost := 12 // Adjust cost as needed (higher is more secure but slower)
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(password), cost)
	if err != nil {
		http.Error(w, "Error hashing password", http.StatusInternalServerError)
		return
	}
	hashedPassword := string(hashedBytes)

	// Insert new user into database
	insertStmt := `INSERT INTO users (username, password_hash) VALUES (?, ?)`
	res, err := db.Exec(insertStmt, username, hashedPassword)
	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := res.RowsAffected()
	if err != nil || rowsAffected == 0 {
		http.Error(w, "User registration failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "User registered successfully")
}
