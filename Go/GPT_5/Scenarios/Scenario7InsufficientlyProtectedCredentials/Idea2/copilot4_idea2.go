package main

import (
    "context"
    "database/sql"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/register", registerHandler)
    // NOTE: In production run behind TLS (ListenAndServeTLS) or reverse proxy that terminates TLS.
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to mitigate large payloads
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // Read and normalize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // Username: allow letters, numbers and underscore, 3-30 chars
    var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_]{3,30}$`)
    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Password policy: minimum length, reasonable maximum
    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    // Obtain DSN from environment to avoid embedding credentials in source
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        http.Error(w, "server configuration error", http.StatusInternalServerError)
        return
    }

    // Open DB (closed after use). In a real app, reuse a global *sql.DB.
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Hash password using bcrypt with an appropriate cost
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), 12)
    // Clear password variable as soon as practical
    password = ""
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Parameterized query to prevent SQL injection
    query := "INSERT INTO users (username, password_hash) VALUES (?, ?)"
    if _, err := db.ExecContext(ctx, query, username, hashed); err != nil {
        // Avoid leaking internal errors. Detect common duplicate-entry case and return 409.
        if strings.Contains(err.Error(), "Duplicate") || strings.Contains(err.Error(), "duplicate") {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Success: do not return any sensitive details
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("registration successful"))
}
