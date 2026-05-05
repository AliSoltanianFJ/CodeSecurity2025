package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
var db *sql.DB

func init() {
    // Initialize DB pool once. DB_DSN must be provided via environment securely (do not hardcode).
    // Example DSN: "username:password@tcp(db-host:3306)/dbname?parseTime=true&tls=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }
    // Connection pool tuning - follow least privilege and resource limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // Verify connectivity with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("failed to ping db: %v", err)
    }
}

// ...existing code...
func main() {
    http.HandleFunc("/register", registerHandler)

    // NOTE: Run behind TLS in production (e.g., use ListenAndServeTLS or a reverse proxy terminating TLS).
    // Enforce HSTS, CSP, secure cookies, and rate limiting at the front door.
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Require a known content type; reject others to reduce attack surface.
    ct := r.Header.Get("Content-Type")
    if ct != "application/x-www-form-urlencoded" && ct != "application/json" {
        http.Error(w, "Unsupported Media Type", http.StatusUnsupportedMediaType)
        return
    }

    // Parse form data safely (limits applied by default values; you may set r.Body size limit upstream)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation (whitelist)
    if err := validateUsername(username); err != nil {
        http.Error(w, fmt.Sprintf("invalid username: %v", err), http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, fmt.Sprintf("invalid password: %v", err), http.StatusBadRequest)
        return
    }

    // Check if user already exists (parameterized query)
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var existingID int64
    err := db.QueryRowContext(ctx, "SELECT id FROM users WHERE username = ?", username).Scan(&existingID)
    if err == nil {
        // user exists
        http.Error(w, "username already taken", http.StatusConflict)
        return
    }
    if err != nil && err != sql.ErrNoRows {
        // unexpected DB error
        log.Printf("db error checking user existence: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Hash password with bcrypt (proper KDF). Cost 12 is a reasonable default; tune for your environment.
    const bcryptCost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Store user record using a prepared statement and context timeout
    txCtx, txCancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer txCancel()

    stmt, err := db.PrepareContext(txCtx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        log.Printf("db prepare error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    res, err := stmt.ExecContext(txCtx, username, hashed, time.Now().UTC())
    if err != nil {
        // handle duplicate key race (best effort)
        if isDuplicateEntryErr(err) {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        log.Printf("db exec error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    uid, _ := res.LastInsertId()

    // Security headers (if TLS is in use)
    if r.TLS != nil {
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }
    // Avoid echoing input back. Return minimal info.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "user created with id %d", uid)
}

// validateUsername enforces a strict whitelist for usernames.
func validateUsername(u string) error {
    if u == "" {
        return errors.New("empty")
    }
    // allowed: 3-30 chars, letters, numbers, dot, underscore, hyphen
    if len(u) < 3 || len(u) > 30 {
        return errors.New("length must be 3-30")
    }
    var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validUser.MatchString(u) {
        return errors.New("contains invalid characters")
    }
    return nil
}

// validatePassword enforces minimum complexity and length.
func validatePassword(p string) error {
    if len(p) < 12 {
        return errors.New("must be at least 12 characters")
    }
    // Further complexity checks can be added (e.g., entropy estimate) but avoid overly strict rules that encourage unsafe reuse.
    return nil
}

// isDuplicateEntryErr tries to detect MySQL duplicate entry errors.
// Keep this simple to avoid importing vendor-specific error types.
func isDuplicateEntryErr(err error) bool {
    if err == nil {
        return false
    }
    // MySQL duplicate entry contains "Error 1062" in driver error string
    return regexp.MustCompile(`(?i)1062|duplicate`).MatchString(err.Error())
}
