package main

import (
    "database/sql"
    "encoding/json"
    "os"
    "errors"
    "fmt"
    "net/http"
    "regexp"
    "context"
    "strings"
    "time"

    mysql "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Secure response headers (defense-in-depth)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    // Only allow POST JSON
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if ct := r.Header.Get("Content-Type"); !strings.HasPrefix(ct, "application/json") {
        http.Error(w, "unsupported media type", http.StatusUnsupportedMediaType)
        return
    }

    // Basic same-origin check to reduce CSRF risk for browser-based clients.
    // This is not a substitute for an application CSRF token for browser forms.
    origin := r.Header.Get("Origin")
    if origin != "" {
        // allow local testing on localhost; in production compare to canonical origin(s)
        if !strings.HasPrefix(origin, "https://"+r.Host) && !strings.HasPrefix(origin, "http://localhost") {
            http.Error(w, "forbidden", http.StatusForbidden)
            return
        }
    }

    // Parse input with strict fields
    var req struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&req); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    // Input validation (whitelist)
    if err := validateCredentials(req.Username, req.Password); err != nil {
        // Do not reveal which field failed to avoid info leakage
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }

    // Hash password using bcrypt with increased cost. Do not use plain SHA or unsalted hashes.
    const bcryptCost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcryptCost)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    // Zero out password variable as best effort (Go strings are immutable; this helps at least for []byte).
    req.Password = ""

    // DB connection: DSN must come from environment or secrets manager.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // Avoid exposing internals to caller
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }

    // Use context with deadline to avoid hanging DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }
    // Ensure DB connection is closed; in a real app, use a global pool created at startup.
    defer db.Close()

    // Optional: enforce ping within the timeout
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }

    // Parameterized INSERT to avoid SQL injection
    stmt := `INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)`
    _, err = db.ExecContext(ctx, stmt, req.Username, string(hashed), time.Now().UTC())
    if err != nil {
        // Handle duplicate username cleanly
        var mysqlErr *mysql.MySQLError
        if errors.As(err, &mysqlErr) && mysqlErr.Number == 1062 {
            http.Error(w, "conflict", http.StatusConflict)
            return
        }
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprint(w, `{"status":"created"}`)
}

// validateCredentials enforces strict whitelist for username and a strong password policy.
func validateCredentials(username, password string) error {
    // Username: 3-30 chars, letters, digits, underscore
    usernameRe := regexp.MustCompile(`^[a-zA-Z0-9_]{3,30}$`)
    if !usernameRe.MatchString(username) {
        return fmt.Errorf("invalid username")
    }
    // Password: minimum length and complexity checks (adjust policy as required)
    if len(password) < 12 {
        return fmt.Errorf("password too short")
    }
    var (
        hasLower  = regexp.MustCompile(`[a-z]`).MatchString
        hasUpper  = regexp.MustCompile(`[A-Z]`).MatchString
        hasNumber = regexp.MustCompile(`[0-9]`).MatchString
        hasSymbol = regexp.MustCompile(`[^a-zA-Z0-9]`).MatchString
    )
    if !(hasLower(password) && hasUpper(password) && hasNumber(password) && hasSymbol(password)) {
        return fmt.Errorf("password lacks complexity")
    }
    return nil
}
