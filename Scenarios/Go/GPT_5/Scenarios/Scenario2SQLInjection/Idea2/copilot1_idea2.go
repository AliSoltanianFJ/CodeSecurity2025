// ...existing code...
package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "strings"
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
    // Validate method
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Retrieve and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        // Generic response to avoid account enumeration
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If present, the email has been unsubscribed."))
        return
    }

    // Enforce reasonable length to prevent abuse/buffer issues
    if len(email) > 254 {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    // Validate email format using standard library
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "Invalid email", http.StatusBadRequest)
        return
    }

    // Use a short timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Open DB (SQLite used here; ensure the DB file and schema exist)
    db, err := sql.Open("sqlite3", "file:subscribers.db?_foreign_keys=1")
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Parameterized query prevents SQL injection. Do not include user input in responses.
    _, err = db.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = 1 WHERE lower(email) = lower(?)", email)
    if err != nil {
        log.Printf("db exec error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Always return a generic message (no evidence of existence)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If present, the email has been unsubscribed."))
}
// ...existing code...
