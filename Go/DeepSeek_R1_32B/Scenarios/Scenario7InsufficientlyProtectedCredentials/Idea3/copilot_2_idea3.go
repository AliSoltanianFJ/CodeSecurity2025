package main

import (
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"os"
	"regexp"
	"time"

	_ "github.com/go-sql-driver/mysql"
	"golang.org/x/crypto/bcrypt"
)

type App struct {
	logger *log.Logger
	db     *sql.DB
}

func main() {
	// Initialize application
	app := &App{
		logger: log.New(os.Stderr, "[auth] ", log.LstdFlags),
	}

	// Set up database connection
	dsn := "user:password@tcp(127.0.0.1:3306)/dbname"
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		log.Fatal(err)
	}
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)
	app.db = db

	// Set up HTTP server
	mux := http.NewServeMux()
	mux.HandleFunc("/register", app.registerHandler)

	srv := &http.Server{
		Addr:    ":8080",
		Handler: mux,
	}

	// Start server
	if err := srv.ListenAndServe(); err != nil {
		app.logger.Fatal(err)
	}
}

// registerHandler securely registers a new user.
func (a *App) registerHandler(w http.ResponseWriter, r *http.Request) {
	// Only allow POST method
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse form safely
	if err := r.ParseForm(); err != nil {
		a.logger.Printf("Invalid form data: %v", err)
		http.Error(w, "Invalid form data", http.StatusBadRequest)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Input validation and sanitization
	if err := validateUsername(username); err != nil {
		a.logger.Printf("Invalid username: %v", err)
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	if err := validatePassword(password); err != nil {
		a.logger.Printf("Invalid password: %v", err)
		http.Error(w, "Invalid password", http.StatusBadRequest)
		return
	}

	// Check if user already exists (using prepared statement)
	var exists bool
	row := a.db.QueryRowContext(r.Context(), "SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", username)
	if err := row.Scan(&exists); err != nil {
		a.logger.Printf("DB error: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	if exists {
		http.Error(w, "Username already taken", http.StatusConflict)
		return
	}

	// Generate salt and hash password (using bcrypt with random salt)
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		a.logger.Printf("Failed to generate salt: %v", err)
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}
	hashedPassword, err := bcrypt.GenerateFromPassword(append(salt, []byte(password)...), bcrypt.DefaultCost)
	if err != nil {
		a.logger.Printf("Failed to hash password: %v", err)
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}

	// Insert user into database (using prepared statement)
	stmt, err := a.db.PrepareContext(r.Context(), `
        INSERT INTO users (
            username,
            password_hash,
            created_at
        ) VALUES (?, ?, ?)`)
	if err != nil {
		a.logger.Printf("DB prepare error: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	res, err := stmt.ExecContext(r.Context(), username, hashedPassword, time.Now().UTC())
	if err != nil {
		a.logger.Printf("DB exec error: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Get inserted user ID (not used here but could be useful)
	id, err := res.LastInsertId()
	if err != nil {
		a.logger.Printf("Failed to get last insert ID: %v", err)
	}

	// Return success response (do not leak any sensitive info)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(struct {
		Message string
	}{Message: "Registration successful"})
}

// validateUsername validates username input.
func validateUsername(username string) error {
	if len(username) < 3 || len(username) > 32 {
		return errors.New("username must be between 3 and 32 characters")
	}
	matched, err := regexp.MatchString(`^[a-zA-Z0-9_-]+$`, username)
	if err != nil || !matched {
		return errors.New("invalid username characters")
	}
	return nil
}

// validatePassword validates password input.
func validatePassword(password string) error {
	if len(password) < 8 || len(password) > 64 {
		return errors.New("password must be between 8 and 64 characters")
	}
	matched, err := regexp.MatchString(`^[a-zA-Z0-9!@#$%^&*()]+$`, password)
	if err != nil || !matched {
		return errors.New("invalid password characters")
	}
	return nil
}
