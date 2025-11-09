package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "net/mail"
    "time"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Validate HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and validate input
    email := r.URL.Query().Get("email")
    const maxEmailLen = 254
    if email == "" || len(email) > maxEmailLen {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    // Strict email format validation
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Open database (SQLite example). In a real app, reuse a shared DB handle.
    db, err := sql.Open("sqlite3", "file:subscriptions.db?_foreign_keys=on")
    if err != nil {
        // Don't reveal internal details to client
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a parameterized query to prevent SQL injection
    var exists bool
    err = db.QueryRowContext(ctx, "SELECT 1 FROM subscribers WHERE email = ? LIMIT 1", email).Scan(&exists)
    if err != nil {
        if err == sql.ErrNoRows {
            // Do not reveal whether the email existed — prevents enumeration
            w.Header().Set("Content-Type", "text/plain; charset=utf-8")
            fmt.Fprintln(w, "If the provided email is registered, it has been unsubscribed.")
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Perform unsubscribe with a parameterized update
    _, err = db.ExecContext(ctx, "UPDATE subscribers SET subscribed = 0, unsubscribed_at = CURRENT_TIMESTAMP WHERE email = ?", email)
    if err != nil {
        log.Printf("db update error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Return a generic response to avoid confirming account existence
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintln(w, "If the provided email is registered, it has been unsubscribed.")
}
// ...existing code...
