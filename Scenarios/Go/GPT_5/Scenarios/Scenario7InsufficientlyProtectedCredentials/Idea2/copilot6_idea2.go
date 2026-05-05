package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"
    "unicode"

    "golang.org/x/crypto/bcrypt"
    mysql "github.com/go-sql-driver/mysql"
)

/* ...existing code... */

var db *sql.DB

func main() {
    // initialize DB from environment variable to avoid hardcoding credentials
    if err := initDB(); err != nil {
        log.Fatalf("service startup failed")
    }

    http.HandleFunc("/register", registerHandler)

    // Use a clear address and avoid exposing internal errors
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

func initDB() error {
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        return fmt.Errorf("missing DATABASE_DSN")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        // Do not expose DSN or sensitive details
        log.Printf("db open error")
        return err
    }

    // sensible connection pooling
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error")
        return err
    }
    return nil
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers to reduce XSS risk and other attacks
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit request body to prevent resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Validate and sanitize username: allow only safe characters and length limits
    if !validUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Validate password strength and length
    if err := validatePassword(password); err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }

    // Hash password with bcrypt (adaptive, salted). Do not use fast hashes like SHA256.
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // internal error - do not reveal details
        log.Println("password hash error")
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // store user using parameterized query to prevent SQL injection
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        log.Println("db prepare error")
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed, time.Now().UTC())
    if err != nil {
        // detect duplicate username (MySQL error 1062) without leaking DB internals
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        log.Println("db exec error")
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Success - do not echo private info back
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("user created"))
}

// validUsername enforces a strict username policy (alphanumeric, underscore, hyphen)
func validUsername(u string) bool {
    if len(u) < 3 || len(u) > 32 {
        return false
    }
    var validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    return validUsername.MatchString(u)
}

// validatePassword enforces minimal password complexity.
// Returns nil if acceptable or an error describing the issue.
func validatePassword(p string) error {
    if len(p) < 8 {
        return fmt.Errorf("password must be at least 8 characters")
    }
    if len(p) > 128 {
        return fmt.Errorf("password too long")
    }

    var hasUpper, hasLower, hasNumber, hasSymbol bool
    for _, r := range p {
        switch {
        case unicode.IsUpper(r):
            hasUpper = true
        case unicode.IsLower(r):
            hasLower = true
        case unicode.IsDigit(r):
            hasNumber = true
        default:
            // treat other printable chars as symbol
            if !unicode.IsSpace(r) {
                hasSymbol = true
            }
        }
    }
    if !(hasUpper && hasLower && hasNumber) {
        return fmt.Errorf("password must include upper, lower, and numeric characters")
    }
    // symbol is recommended but optional; could be enforced if desired
    return nil
}

/* ...existing code... */
