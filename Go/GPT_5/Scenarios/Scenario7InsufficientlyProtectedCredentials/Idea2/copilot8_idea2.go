// ...existing code...
package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // Read DSN from environment to avoid embedding secrets in source
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatalf("missing MYSQL_DSN environment variable")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    // Configure sensible pool limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(5)

    // Verify connection with a timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("db ping: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    // Note: serve behind a TLS-terminating proxy in production
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (basic)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none';")
    w.Header().Set("X-XSS-Protection", "0") // modern browsers prefer CSP; XSS filter legacy

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit the size of the request body to prevent abuse
    r.Body = http.MaxBytesReader(w, r.Body, 10<<20) // 10 MiB

    // Parse form data
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate username: 3-32 chars, letters, numbers, underscore, hyphen
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }
    if len(username) < 3 || len(username) > 32 {
        http.Error(w, "username must be between 3 and 32 characters", http.StatusBadRequest)
        return
    }
    userRe := regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !userRe.MatchString(username) {
        http.Error(w, "username contains invalid characters", http.StatusBadRequest)
        return
    }

    // Validate password: require reasonable minimum length and max cap
    if password == "" {
        http.Error(w, "password required", http.StatusBadRequest)
        return
    }
    if len(password) < 12 {
        http.Error(w, "password must be at least 12 characters", http.StatusBadRequest)
        return
    }
    if len(password) > 256 {
        http.Error(w, "password too long", http.StatusBadRequest)
        return
    }
    // Optionally, ensure password contains a mix (not enforced here to avoid bad UX)

    // Hash password with bcrypt (secure, salted hashing)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // Log server-side; do not reveal internal details to client
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Store user record using parameterized query and a short DB context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    const query = `INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())`
    _, err = db.ExecContext(ctx, query, username, string(hashed))
    if err != nil {
        // Handle duplicate username in a safe way
        var myErr *mysql.MySQLError
        if ok := errorAs(err, &myErr); ok && myErr.Number == 1062 {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        // Log and return a generic error
        log.Printf("db insert error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Successful creation: do not include any sensitive data in the response
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "registration successful")
}

// errorAs is a tiny helper to avoid importing errors in Go versions that may differ.
// Prefer errors.As from the standard library when available.
func errorAs(err error, target interface{}) bool {
    switch t := target.(type) {
    case **mysql.MySQLError:
        if e, ok := err.(*mysql.MySQLError); ok {
            *t = e
            return true
        }
    }
    return false
}
// ...existing code...
