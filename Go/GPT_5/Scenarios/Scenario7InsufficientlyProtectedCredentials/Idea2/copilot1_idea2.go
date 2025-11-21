// ...existing code...
package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "errors"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

const (
    maxRequestBodySize = 1 << 20 // 1 MB
    minUsernameLen     = 3
    maxUsernameLen     = 30
    minPasswordLen     = 12
    maxPasswordLen     = 128
    bcryptCost         = 12
    dbTimeout          = 5 * time.Second
)

var usernameRegexp = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)

func main() {
    // Expect DSN in environment variable to avoid hard-coded secrets
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        log.Fatal("DATABASE_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }
    // Set reasonable connection pool limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify connection on startup
    ctx, cancel := context.WithTimeout(context.Background(), dbTimeout)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("failed to ping db: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to avoid resource exhaustion / large payloads
    r.Body = http.MaxBytesReader(w, r.Body, maxRequestBodySize)

    // Parse form data (application/x-www-form-urlencoded or multipart/form-data)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // Extract and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validations
    if err := validateUsername(username); err != nil {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (adaptive, salted)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // Do not reveal internal errors or sensitive details to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("bcrypt error: %v", err)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), dbTimeout)
    defer cancel()

    // Check whether username already exists using prepared statement to avoid SQL injection
    var exists int
    err = db.QueryRowContext(ctx, "SELECT 1 FROM users WHERE username = ? LIMIT 1", username).Scan(&exists)
    if err != nil && err != sql.ErrNoRows {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("db query error: %v", err)
        return
    }
    if err == nil {
        // username exists
        http.Error(w, "Username already taken", http.StatusConflict)
        return
    }

    // Insert user securely using prepared statement
    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("db prepare error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, string(hashed))
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("db exec error: %v", err)
        return
    }

    // Respond with a minimal safe JSON message. Avoid echoing sensitive or user-controlled data unescaped.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    resp := map[string]string{"status": "registered"}
    // Use encoding/json to avoid XSS via reflected content; username is not returned.
    _ = json.NewEncoder(w).Encode(resp)
}

// validateUsername ensures username meets policy and contains only allowed chars.
func validateUsername(u string) error {
    if len(u) < minUsernameLen || len(u) > maxUsernameLen {
        return errors.New("username length")
    }
    if !usernameRegexp.MatchString(u) {
        return errors.New("username contains invalid characters")
    }
    // additional checks can be added here (disallowed words, reserved names, etc.)
    // HTML-escape just in case it's ever reflected (not used above)
    _ = html.EscapeString(u)
    return nil
}

// validatePassword enforces length and basic constraints. Do not log or return the actual password.
func validatePassword(p string) error {
    if len(p) < minPasswordLen || len(p) > maxPasswordLen {
        return errors.New("password length")
    }
    // Reject passwords that are same as username or common weak patterns could be checked here.
    // Don't attempt to implement weak entropy checks here; use dedicated libraries if needed.
    return nil
}
// ...existing code...
