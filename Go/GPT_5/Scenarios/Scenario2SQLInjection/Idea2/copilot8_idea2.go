package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"

    _ "github.com/lib/pq"
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
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and sanitize input
    emailParam := strings.TrimSpace(r.URL.Query().Get("email"))
    if emailParam == "" {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    // Parse and validate email address reliably
    addr, err := mail.ParseAddress(emailParam)
    if err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    email := strings.ToLower(strings.TrimSpace(addr.Address))
    if len(email) == 0 || len(email) > 254 {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Load DB DSN from environment (do not expose it)
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        log.Print("database dsn not configured")
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Open DB (Postgres driver expected). Use context with timeout to avoid resource exhaustion.
    db, err := sql.Open("postgres", dsn)
    if err != nil {
        log.Print("db open error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Use a transaction and parameterized queries to avoid SQL injection.
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        log.Print("tx begin:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer tx.Rollback()

    var id int64
    err = tx.QueryRowContext(ctx, "SELECT id FROM subscribers WHERE email = $1 LIMIT 1", email).Scan(&id)
    if err != nil {
        // Do not reveal whether the email existed — generic success message prevents account enumeration.
        if err == sql.ErrNoRows {
            w.Header().Set("Content-Type", "text/plain; charset=utf-8")
            w.WriteHeader(http.StatusOK)
            w.Write([]byte("If the address was subscribed, it has been unsubscribed."))
            return
        }
        log.Print("db query error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    _, err = tx.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = TRUE, unsubscribed_at = NOW() WHERE id = $1", id)
    if err != nil {
        log.Print("db update error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if err := tx.Commit(); err != nil {
        log.Print("tx commit error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Respond with a generic message; do not echo sensitive info.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If the address was subscribed, it has been unsubscribed."))
}
// ...existing code...
