package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    db                *sql.DB
    usernameWhitelist = regexp.MustCompile(`^[A-Za-z0-9_]{3,30}$`) // strict whitelist for usernames
    minPasswordLen    = 12
)

// main initializes DB from environment and starts HTTP server.
// Security notes:
// - DB DSN must come from an environment variable (secrets manager in prod).
// - The DB user should have least-privilege (only necessary rights on users table).
// - Consider running this behind a TLS-terminating reverse proxy in production.
func main() {
    dsn := strings.TrimSpace(os.Getenv("MYSQL_DSN"))
    if dsn == "" {
        // Fail fast: do not run with embedded credentials or defaults
        fmt.Fprintln(os.Stderr, "Fatal: MYSQL_DSN not set")
        os.Exit(2)
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        fmt.Fprintln(os.Stderr, "Fatal: db open:", err)
        os.Exit(2)
    }
    // Connection pool and limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // Lightweight health check endpoint could be added (not exposing internals)
    http.HandleFunc("/register", registerHandler)

    // REQUIRE_TLS can be set to "true" in prod to refuse non-TLS requests
    fmt.Println("Starting server on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Fprintln(os.Stderr, "Server fatal:", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST for registration
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Optional: enforce TLS in production by setting REQUIRE_TLS=true
    if strings.ToLower(os.Getenv("REQUIRE_TLS")) == "true" && r.TLS == nil {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // Limit request body size to avoid large payload abuse
    r.Body = http.MaxBytesReader(w, r.Body, 10<<10) // 10 KB limit for form payloads

    // Parse form values (works for application/x-www-form-urlencoded)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate inputs: canonicalize and whitelist username, enforce password policy
    if err := validateUsername(username); err != nil {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, "Weak password", http.StatusBadRequest)
        return
    }

    // Canonicalize username (optional): lowercasing to avoid duplicates differing by case
    username = strings.ToLower(username)

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Check for existing user (parameterized query prevents SQLi)
    var exists bool
    if err := db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", username).Scan(&exists); err != nil {
        // Do not leak DB internals to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if exists {
        // Do not distinguish too much between auth errors to avoid user enumeration; using 409 here is acceptable
        http.Error(w, "User already exists", http.StatusConflict)
        return
    }

    // Hash password using bcrypt (adaptive, not a fast hash). Cost 12 is a reasonable starting point.
    const bcryptCost = 12
    hashedPW, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Insert user using parameterized statement (no concatenation)
    tx, err := db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelReadCommitted})
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    stmt := "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, UTC_TIMESTAMP())"
    if _, err := tx.ExecContext(ctx, stmt, username, string(hashedPW)); err != nil {
        _ = tx.Rollback()
        // Possible conflict due to race; report generic error to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if err := tx.Commit(); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Success: minimal response; do NOT include sensitive details
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User created")
}

// validateUsername enforces a strict whitelist and length; prevents injection via username.
func validateUsername(u string) error {
    if u == "" {
        return errors.New("empty username")
    }
    if !usernameWhitelist.MatchString(u) {
        return errors.New("username contains invalid characters or wrong length")
    }
    return nil
}

// validatePassword enforces a minimum length and character classes.
// This is a policy decision; consider using a zxcvbn-based entropy check for better UX/security.
func validatePassword(p string) error {
    if len(p) < minPasswordLen {
        return fmt.Errorf("password too short")
    }
    var hasLower, hasUpper, hasDigit, hasSpecial bool
    for _, r := range p {
        switch {
        case r >= 'a' && r <= 'z':
            hasLower = true
        case r >= 'A' && r <= 'Z':
            hasUpper = true
        case r >= '0' && r <= '9':
            hasDigit = true
        default:
            // treat remaining printable characters as special
            hasSpecial = true
        }
    }
    if !hasLower || !hasUpper || !hasDigit || !hasSpecial {
        return fmt.Errorf("password must include upper, lower, digit, and special characters")
    }
    return nil
}
