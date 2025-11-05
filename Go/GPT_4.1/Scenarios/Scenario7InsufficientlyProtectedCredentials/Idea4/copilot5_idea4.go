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

// Security decision: Use environment variables for DB credentials, never hardcode secrets.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbName     = os.Getenv("DB_NAME")
)

// Security decision: Use strict input validation regex for usernames (alphanumeric, 3-32 chars).
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

// Security decision: Enforce strong password policy (min 12 chars, at least 1 upper, 1 lower, 1 digit, 1 special).
var passwordRegex = regexp.MustCompile(`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{12,}$`)

func main() {
    // Security decision: Only listen on localhost for development; use HTTPS in production.
    http.HandleFunc("/register", registerHandler)
    log.Println("Server started on http://localhost:8080 (use HTTPS in production)")
    if err := http.ListenAndServe("127.0.0.1:8080", nil); err != nil {
        log.Fatalf("Server failed: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security decision: Limit request body size to prevent DoS attacks.
    r.Body = http.MaxBytesReader(w, r.Body, 4096)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation and sanitization
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username format", http.StatusBadRequest)
        return
    }
    if !passwordRegex.MatchString(password) {
        http.Error(w, "Password does not meet complexity requirements", http.StatusBadRequest)
        return
    }

    // Security decision: Use bcrypt for password hashing with default cost.
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("bcrypt error: %v", err)
        return
    }

    // Security decision: Use parameterized queries to prevent SQL injection.
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=false", dbUser, dbPassword, dbHost, dbName)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB connection error: %v", err)
        return
    }
    defer db.Close()

    // Security decision: Use context with timeout for DB operations.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Check if username already exists (avoid user enumeration by generic error).
    var exists int
    err = db.QueryRowContext(ctx, "SELECT COUNT(*) FROM users WHERE username = ?", username).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }
    if exists > 0 {
        http.Error(w, "Registration failed", http.StatusBadRequest)
        return
    }

    // Insert new user securely.
    _, err = db.ExecContext(ctx,
        "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)",
        username, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB insert error: %v", err)
        return
    }

    // Security decision: Encode output to prevent XSS.
    fmt.Fprintf(w, "Registration successful for user: %s", html.EscapeString(username))
}
