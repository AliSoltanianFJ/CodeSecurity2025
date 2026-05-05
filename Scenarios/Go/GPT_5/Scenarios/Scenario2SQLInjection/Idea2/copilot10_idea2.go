package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"

    _ "github.com/lib/pq"
)

// ...existing code...
var db *sql.DB

func init() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("postgres", dsn) // ensure you use a supported driver in your go.mod
    if err != nil {
        log.Fatal("db open:", err)
    }

    db.SetMaxOpenConns(10)
    db.SetConnMaxIdleTime(5 * time.Minute)

    if err = db.Ping(); err != nil {
        log.Fatal("db ping:", err)
    }
}

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    // Limit length to avoid unusually large inputs
    if len(email) > 254 {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    // Validate email format with standard library (prevents malformed input)
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    email = strings.ToLower(email)

    // Use a context timeout to prevent resource exhaustion
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Parameterised query prevents SQL injection; do not concatenate user input into SQL
    var exists bool
    err := db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = $1 AND unsubscribed = FALSE)", email).Scan(&exists)
    if err != nil {
        log.Println("db query error:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Generic response to avoid leaking whether an address existed (prevents information disclosure)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if !exists {
        fmt.Fprint(w, "If that email was subscribed, it has been unsubscribed.")
        return
    }

    // Update using parameterised query
    if _, err := db.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = TRUE, unsubscribed_at = now() WHERE email = $1", email); err != nil {
        log.Println("db update error:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Server-side logging only; do not expose sensitive details to clients
    log.Printf("Processed unsubscribe for email (server-side) successfully.")
    fmt.Fprint(w, "If that email was subscribed, it has been unsubscribed.")
}
// ...existing code...
