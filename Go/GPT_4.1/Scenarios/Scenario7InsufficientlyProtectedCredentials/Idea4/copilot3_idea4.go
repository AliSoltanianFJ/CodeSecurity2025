package main

import (
    "database/sql"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// Security: Use environment variables for DB credentials, never hardcode secrets.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbName     = os.Getenv("DB_NAME")
)

// Security: Use a strict username policy (alphanumeric, 3-32 chars)
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

// Security: Enforce strong password policy (min 12 chars, at least one upper, lower, digit, special)
var passwordRegex = regexp.MustCompile(`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{12,}$`)

func main() {
    // Security: Always use HTTPS in production. This is for demonstration only.
    http.HandleFunc("/register", registerHandler)
    log.Println("Listening on :8080 (HTTP, for demo only; use HTTPS in production)")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("Server error: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB max

    // Parse form data securely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Security: Validate and sanitize user input
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !passwordRegex.MatchString(password) {
        http.Error(w, "Password does not meet complexity requirements", http.StatusBadRequest)
        return
    }
    // Security: HTML-escape username to prevent XSS in any output
    safeUsername := html.EscapeString(username)

    // Security: Hash password with bcrypt (strong, salted, adaptive)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("bcrypt error: %v", err)
        return
    }

    // Security: Use parameterized queries to prevent SQL injection
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=false", dbUser, dbPassword, dbHost, dbName)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB connection error: %v", err)
        return
    }
    defer db.Close()

    // Security: Set connection limits (least privilege)
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)

    // Security: Check for existing username (avoid duplicate accounts)
    var exists int
    err = db.QueryRow("SELECT COUNT(*) FROM users WHERE username = ?", safeUsername).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }
    if exists > 0 {
        http.Error(w, "Username already exists", http.StatusConflict)
        return
    }

    // Security: Insert new user with prepared statement
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB prepare error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(safeUsername, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB exec error: %v", err)
        return
    }

    // Security: Do not leak sensitive info in responses
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "Registration successful")
}

// Security: Never expose stack traces or sensitive errors to clients.
// Security: Always use HTTPS in production to protect credentials in transit.
// Security: Use strong password hashing (bcrypt) and input validation to prevent common attacks.
