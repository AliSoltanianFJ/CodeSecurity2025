package main

import (
    "context"
    "database/sql"
    "fmt"
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

// ...existing code...
var db *sql.DB
var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_]{3,30}$`)

func main() {
    // ...existing code...
    dsn := os.Getenv("MYSQL_DSN") // expect e.g. "user:pass@tcp(127.0.0.1:3306)/dbname"
    if strings.TrimSpace(dsn) == "" {
        log.Fatal("database DSN not configured (set MYSQL_DSN)")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("failed to open database") // don't print DSN or other secrets
    }
    // Set reasonable connection limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // verify connection with a short timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatal("database unreachable")
    }

    http.HandleFunc("/register", registerHandler)
    // Note: run behind TLS in production (reverse proxy or ListenAndServeTLS)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Limit request size to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation & sanitization
    if username == "" || password == "" {
        http.Error(w, "Missing username or password", http.StatusBadRequest)
        return
    }
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Password must be between 8 and 128 characters", http.StatusBadRequest)
        return
    }
    if !usernameRE.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    // Escape kept only if rendering back to HTML (we don't echo it back here).
    safeUsername := html.EscapeString(username)

    // Use a context with timeout for DB ops
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Check if user already exists (parameterized query to prevent SQL injection)
    var existingID int
    err := db.QueryRowContext(ctx, "SELECT id FROM users WHERE username = ? LIMIT 1", username).Scan(&existingID)
    if err != nil && err != sql.ErrNoRows {
        // don't leak DB errors to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if err == nil {
        // user exists
        http.Error(w, "Username already taken", http.StatusConflict)
        return
    }

    // Hash password with bcrypt (work factor chosen reasonably)
    const bcryptCost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Insert user with parameterized statement
    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed)
    if err != nil {
        // avoid revealing DB details
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Success response - do not include sensitive data or raw user input
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    // minimal JSON response
    fmt.Fprint(w, `{"status":"created"}`)
}
// ...existing code...
