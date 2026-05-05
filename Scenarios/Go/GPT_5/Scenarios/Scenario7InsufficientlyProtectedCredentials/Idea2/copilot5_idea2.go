// ...existing code...
package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"
    "unicode"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    // Read DSN from environment to avoid hardcoding credentials
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        log.Fatal("DATABASE_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }
    // Set reasonable connection pool limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxIdleConns(2)
    db.SetMaxOpenConns(10)

    // Verify DB connectivity with a timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("failed to connect to database: %v", err)
    }
    defer db.Close()

    http.HandleFunc("/register", registerHandler)
    // Note: production must use HTTPS and proper TLS termination
    log.Println("server starting on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

// ...existing code...
/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers to help mitigate XSS & other common issues
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB

    // Only accept typical form content types for this endpoint
    ct := r.Header.Get("Content-Type")
    if ct == "" {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    // Allow form submissions (application/x-www-form-urlencoded or multipart/form-data)
    // Do not accept arbitrary content types to avoid unsafe deserialization
    if !(regexp.MustCompile(`^application\/x-www-form-urlencoded`).MatchString(ct) ||
        regexp.MustCompile(`^multipart\/form-data`).MatchString(ct)) {
        http.Error(w, "unsupported content type", http.StatusUnsupportedMediaType)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Basic input validation and sanitization
    if !isValidUsername(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if !isValidPassword(password) {
        http.Error(w, "password does not meet policy", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (includes a per-password salt)
    const bcryptCost = 12 // reasonable cost for current environments
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // Log server-side error but don't reveal details to client
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Store user in DB using a prepared statement and context with timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        log.Printf("db prepare error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, string(hashed))
    if err != nil {
        // Try to detect duplicate username (MySQL error 1062)
        if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        log.Printf("db exec error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Return a generic JSON success response (no secret data, no echoing of user input)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "created"})
}

// Username policy: 3-30 chars, letters, numbers, dot, underscore, hyphen
var usernameRE = regexp.MustCompile(`^[a-zA-Z0-9._-]{3,30}$`)

func isValidUsername(s string) bool {
    if len(s) == 0 {
        return false
    }
    // Enforce regex policy
    if !usernameRE.MatchString(s) {
        return false
    }
    return true
}

// Password policy: at least 12 characters, contains lower, upper, digit, and symbol
func isValidPassword(p string) bool {
    if len(p) < 12 || len(p) > 128 {
        return false
    }
    var hasLower, hasUpper, hasDigit, hasSymbol bool
    for _, r := range p {
        switch {
        case unicode.IsLower(r):
            hasLower = true
        case unicode.IsUpper(r):
            hasUpper = true
        case unicode.IsDigit(r):
            hasDigit = true
        default:
            // treat other printable runes as symbols
            hasSymbol = true
        }
    }
    return hasLower && hasUpper && hasDigit && hasSymbol
}
