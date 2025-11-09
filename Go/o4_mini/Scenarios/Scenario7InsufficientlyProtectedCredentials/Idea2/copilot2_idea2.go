package main

import (
    "context"
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
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

    // Limit request size to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic validation and length limits to avoid abuse / buffer issues
    if len(username) < 3 || len(username) > 30 || len(password) < 12 || len(password) > 256 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Allow only safe characters in username
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Get DB DSN from environment (do not hardcode credentials)
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Connection tuning
    db.SetConnMaxLifetime(3 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(10)

    // Use bcrypt for password hashing (secure, salted)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Check for existing user (prepared statement via parameterized query)
    var exists bool
    if err := db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", username).Scan(&exists); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    if exists {
        // Return a clear but non-revealing response
        http.Error(w, "User already exists", http.StatusConflict)
        return
    }

    // Insert user with parameterized statement to prevent SQL injection
    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    if _, err := stmt.ExecContext(ctx, username, hashed); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Do not echo back private data; return a minimal success message
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintln(w, "Registration successful")
}
// ...existing code...
