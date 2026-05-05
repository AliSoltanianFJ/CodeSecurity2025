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

var db *sql.DB

func initDB() error {
	conn := fmt.Sprintf("%s:%s@tcp(%s)/%s", os.Getenv("DB_USER"), os.Getenv("DB_PASSWORD"), os.Getenv("DB_HOST"), os.Getenv("DB_NAME"))
	db, err := sql.Open("mysql", conn)
	if err != nil {
		return err
	}
	return db.Ping()
}

func main() {
	if err := initDB(); err != nil {
		panic(fmt.Sprintf("Failed to connect to database: %v", err))
	}
	defer db.Close()

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

	// Input validation
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if len(username) > 32 || len(username) < 1 {
		http.Error(w, "Username must be between 1-32 characters", http.StatusBadRequest)
		return
	}

	if len(password) < 8 {
		http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
		return
	}

	// Check if username already exists
	var existingUser string
	err := db.QueryRow("SELECT username FROM users WHERE username = ?", username).Scan(&existingUser)
	if err == nil {
		http.Error(w, "Username already taken", http.StatusConflict)
		return
	} else if err != sql.ErrNoRows {
		http.Error(w, "Database error occurred", http.StatusInternalServerError)
		return
	}

	// Password hashing using bcrypt
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), 12)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Store hashed password in database
	stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
	if err != nil {
		http.Error(w, "Database error occurred", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	tx, err := db.Begin()
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	_, err = tx.Stmt(stmt).Exec(username, string(hashedPassword))
	if err != nil {
		tx.Rollback()
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	tx.Commit()

	fmt.Fprintf(w, "Registration successful")
}
