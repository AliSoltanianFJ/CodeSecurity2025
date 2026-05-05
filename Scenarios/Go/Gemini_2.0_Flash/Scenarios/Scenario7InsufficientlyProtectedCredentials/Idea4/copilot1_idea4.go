package main

import (
    "crypto/rand"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

const (
    passwordHashAlgorithm = "SHA-256"
    saltLength            = 32
)

var (
    db *sql.DB
)

func init() {
    // Initialize database connection securely
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
    if err != nil {
        log.Fatalf("Failed to connect to the database: %v", err)
    }
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

    if err := validateInput(username, password); err != nil {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Sanitize input
    username = html.EscapeString(username)

    // Generate a secure salt
    salt, err := generateSalt()
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error generating salt: %v", err)
        return
    }

    // Hash the password with the salt
    hashedPassword := hashPassword(password, salt)

    // Store the user securely in the database
    if err := storeUser(username, hashedPassword, salt); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error storing user: %v", err)
        return
    }

    // Respond to the user
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

func validateInput(username, password string) error {
    // Validate username: alphanumeric, 3-20 characters
    usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
    if !usernameRegex.MatchString(username) {
        return errors.New("invalid username")
    }

    // Validate password: at least 8 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character
    passwordRegex := regexp.MustCompile(`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$`)
    if !passwordRegex.MatchString(password) {
        return errors.New("invalid password")
    }

    return nil
}

func generateSalt() (string, error) {
    salt := make([]byte, saltLength)
    _, err := rand.Read(salt)
    if err != nil {
        return "", err
    }
    return base64.StdEncoding.EncodeToString(salt), nil
}

func hashPassword(password, salt string) string {
    hash := sha256.New()
    hash.Write([]byte(password))
    hash.Write([]byte(salt))
    return base64.StdEncoding.EncodeToString(hash.Sum(nil))
}

func storeUser(username, hashedPassword, salt string) error {
    // Use parameterized queries to prevent SQL injection
    query := "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)"
    stmt, err := db.Prepare(query)
    if err != nil {
        return err
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, salt)
    return err
}
