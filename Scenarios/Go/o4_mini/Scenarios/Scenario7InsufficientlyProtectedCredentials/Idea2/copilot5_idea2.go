// ...existing code...
package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Limit request size to prevent large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password") // do not trim internal spaces for passwords

    // Basic validation and sanitisation
    if username == "" || password == "" {
        http.Error(w, "Missing username or password", http.StatusBadRequest)
        return
    }

    // Enforce reasonable length limits to avoid abuse and buffer issues
    if len(username) < 3 || len(username) > 64 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Allow a safe username character set
    var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Hash password using bcrypt (secure, adaptive)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("password hashing failed: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Obtain DSN from environment; avoid hardcoding credentials in source
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Printf("DB_DSN not configured")
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare statement to prevent SQL injection
    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        log.Printf("db prepare error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed)
    if err != nil {
        // Detect duplicate key for friendly message without leaking details
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        log.Printf("db exec error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Set safe response headers
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    // Do not reflect user input or reveal private information
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("Registration successful"))
}
// ...existing code...
