package main

import (
    "crypto/rand"
    "crypto/sha256"
    "crypto/subtle"
    "database/sql"
    "encoding/base64"
    "errors"
    "fmt"
    "log"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

const (
    // Define password hashing cost
    bcryptCost = 12

    // Define username and password validation rules
    usernameRegex = `^[a-zA-Z0-9_]{3,20}$` // Allow alphanumeric and underscores, 3-20 chars
    passwordMinLen = 12                    // Minimum password length
    passwordMaxLen = 64                    // Maximum password length
)

// Secure database connection (use environment variables or a secrets manager for credentials)
var db *sql.DB

func init() {
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/secure_app")
    if err != nil {
        log.Fatalf("Failed to connect to database: %v", err)
    }
    // Set secure database connection settings
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce HTTPS
    if r.TLS == nil {
        http.Error(w, "HTTPS is required", http.StatusUpgradeRequired)
        return
    }

    // Parse and validate input
    username := r.FormValue("username")
    password := r.FormValue("password")

    if err := validateUsername(username); err != nil {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    if err := validatePassword(password); err != nil {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Hash the password securely
    hashedPassword, err := hashPassword(password)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error hashing password: %v", err)
        return
    }

    // Store the user securely in the database
    if err := storeUser(username, hashedPassword); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error storing user: %v", err)
        return
    }

    // Respond with success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

// Validate username against a strict regex
func validateUsername(username string) error {
    matched, err := regexp.MatchString(usernameRegex, username)
    if err != nil {
        return err
    }
    if !matched {
        return errors.New("username does not meet requirements")
    }
    return nil
}

// Validate password length and complexity
func validatePassword(password string) error {
    if len(password) < passwordMinLen || len(password) > passwordMaxLen {
        return errors.New("password length is invalid")
    }
    // Add additional complexity checks if needed (e.g., uppercase, special chars)
    return nil
}

// Hash the password using bcrypt
func hashPassword(password string) (string, error) {
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        return "", err
    }
    return string(hashed), nil
}

// Store the user securely in the database
func storeUser(username, hashedPassword string) error {
    // Use parameterized queries to prevent SQL injection
    query := "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)"
    _, err := db.Exec(query, username, hashedPassword, time.Now().UTC())
    return err
}

// Securely compare two strings to prevent timing attacks
func secureCompare(a, b string) bool {
    return subtle.ConstantTimeCompare([]byte(a), []byte(b)) == 1
}
