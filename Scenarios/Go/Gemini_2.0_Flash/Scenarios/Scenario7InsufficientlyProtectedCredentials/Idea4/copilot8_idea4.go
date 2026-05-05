package main

import (
    "crypto/rand"
    "crypto/sha256"
    "crypto/subtle"
    "database/sql"
    "encoding/hex"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

const (
    passwordMinLength = 12
    passwordMaxLength = 64
    usernameRegex     = `^[a-zA-Z0-9_]{3,30}$` // Allow only alphanumeric usernames with underscores, 3-30 characters
)

var (
    db *sql.DB // Database connection pool
)

func init() {
    // Securely initialize the database connection
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
    if err != nil {
        log.Fatalf("Failed to connect to the database: %v", err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
}

// registerHandler handles user registration securely
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce secure headers
    w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-XSS-Protection", "1; mode=block")

    // Parse and validate input
    err := r.ParseForm()
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

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
    err = storeUser(username, hashedPassword)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Username already exists", http.StatusConflict)
        } else {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            log.Printf("Error storing user: %v", err)
        }
        return
    }

    // Respond with success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

// validateUsername ensures the username meets security requirements
func validateUsername(username string) error {
    if len(username) == 0 || len(username) > 30 {
        return errors.New("username length invalid")
    }
    matched, err := regexp.MatchString(usernameRegex, username)
    if err != nil || !matched {
        return errors.New("username format invalid")
    }
    return nil
}

// validatePassword ensures the password meets security requirements
func validatePassword(password string) error {
    if len(password) < passwordMinLength || len(password) > passwordMaxLength {
        return errors.New("password length invalid")
    }
    return nil
}

// hashPassword hashes the password using bcrypt
func hashPassword(password string) (string, error) {
    hashedBytes, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        return "", err
    }
    return string(hashedBytes), nil
}

// storeUser securely stores the user in the database
func storeUser(username, hashedPassword string) error {
    // Use parameterized queries to prevent SQL injection
    query := "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)"
    _, err := db.Exec(query, username, hashedPassword, time.Now().UTC())
    return err
}

// secureCompare performs a constant-time comparison of two strings
func secureCompare(a, b string) bool {
    return subtle.ConstantTimeCompare([]byte(a), []byte(b)) == 1
}
