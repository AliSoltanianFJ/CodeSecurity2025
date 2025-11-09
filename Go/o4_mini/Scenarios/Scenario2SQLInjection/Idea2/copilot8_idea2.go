package main

import (
    "context"
    "database/sql"
    _ "modernc.org/sqlite" // pure-Go sqlite driver
    "net/http"
    "net/mail"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Validate and sanitise input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }
    if len(email) > 254 { // RFC max length for address
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Use short context timeout for DB ops
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    // Open database (use a DSN appropriate for your environment)
    db, err := sql.Open("sqlite", "file:subscriptions.db?_foreign_keys=on")
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a transaction and parameterised query to avoid SQL injection
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    // Update without revealing whether the email existed. Use case-insensitive match.
    _, err = tx.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = 1 WHERE LOWER(email) = LOWER(?)", email)
    if err != nil {
        tx.Rollback()
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if err := tx.Commit(); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Generic response to avoid leaking whether the email was present
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If the provided email was subscribed, it has been unsubscribed."))
}
