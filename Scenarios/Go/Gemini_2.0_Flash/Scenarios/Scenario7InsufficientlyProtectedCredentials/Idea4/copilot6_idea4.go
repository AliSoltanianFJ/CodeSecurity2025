package main

import (
    "crypto/rand"
    "crypto/sha256"
    "crypto/subtle"
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
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func init() {
    // Securely initialize the database connection
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
    if err != nil {
        log.Fatalf("Failed to connect to database: %v", err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce HTTPS
    if r.TLS == nil {
        http.Error(w, "HTTPS is required", http.StatusUpgradeRequired)
        return
    }

    // Allow only POST requests
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and validate form input
    err := r.ParseForm()
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Validate username and password
    if !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !isValidPassword(password) {
        http.Error(w, "Password does not meet security requirements", http.StatusBadRequest)
        return
    }

    // Hash the password with bcrypt
    hashedPassword, err := hashPassword(password)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error hashing password: %v", err)
        return
    }

    // Store the user securely in the database
    err = storeUser(username, hashedPassword)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error storing user: %v", err)
        return
    }

    // Respond with success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

func isValidUsername(username string) bool {
    // Allow only alphanumeric usernames between 3 and 30 characters
    re := regexp.MustCompile(`^[a-zA-Z0-9]{3,30}$`)
    return re.MatchString(username)
}

func isValidPassword(password string) bool {
    // Enforce strong password policy: at least 8 characters, including uppercase, lowercase, number, and special character
    re := regexp.MustCompile(`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$`)
    return re.MatchString(password)
}

func hashPassword(password string) (string, error) {
    // Use bcrypt to hash the password with a strong cost factor
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        return "", err
    }
    return string(hashed), nil
}

func storeUser(username, hashedPassword string) error {
    // Use parameterized queries to prevent SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
    if err != nil {
        return err
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword)
    return err
}

func secureCompare(a, b string) bool {
    // Use constant-time comparison to prevent timing attacks
    return subtle.ConstantTimeCompare([]byte(a), []byte(b)) == 1
}
