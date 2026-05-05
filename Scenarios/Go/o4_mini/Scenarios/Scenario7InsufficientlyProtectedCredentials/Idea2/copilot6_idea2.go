// ...existing code...
package main

import (
    "context"
    "database/sql"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,30}$`)

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB

    // Parse form input
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation and sanitisation
    if !usernameRe.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Hash password using bcrypt (secure and salted)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Get database DSN from environment (do not hardcode credentials)
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        http.Error(w, "Server misconfiguration", http.StatusInternalServerError)
        return
    }

    // Open DB and use context with timeout
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use prepared statement to prevent SQL injection
    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed)
    if err != nil {
        // Do not expose DB errors or sensitive details to the client
        http.Error(w, "Unable to register", http.StatusConflict)
        return
    }

    // Do not echo back user-supplied values to avoid XSS
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("User registered"))
}
// ...existing code...
