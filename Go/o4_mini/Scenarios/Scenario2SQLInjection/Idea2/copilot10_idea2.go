// ...existing code...
package main

import (
    "context"
    "database/sql"
    "net/http"
    "net/mail"
    "time"

    _ "modernc.org/sqlite" // pure-Go sqlite driver; replace with your driver if different
)

// ...existing code...
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Accept only GET for this scenario (prefer POST in real apps)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Retrieve and validate input
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }
    // Basic length check to avoid oversized inputs
    if len(email) > 254 {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    // Strict email validation using net/mail
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Open database (use a connection pool in production; this is simple example)
    db, err := sql.Open("sqlite", "file:subscribers.db?_busy_timeout=5000&cache=shared")
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a short context timeout to avoid long-running DB ops
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Parameterised query to prevent SQL injection
    var exists int
    err = db.QueryRowContext(ctx, "SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&exists)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Always return a generic response to avoid user enumeration
    if exists == 0 {
        // Do not reveal that the address was not found
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If the address was subscribed, it has been unsubscribed."))
        return
    }

    // Perform unsubscribe using a parameterised statement
    res, err := db.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = CURRENT_TIMESTAMP WHERE email = ?", email)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    // Optional: check rows affected (not leaked to user)
    _, _ = res.RowsAffected()

    // Generic acknowledgement (do not echo the email)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If the address was subscribed, it has been unsubscribed."))
}
// ...existing code...
