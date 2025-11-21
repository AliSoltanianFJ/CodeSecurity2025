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

    _ "modernc.org/sqlite"
)

var db *sql.DB

func main() {
    var err error
    // Open a safe, modern pure-Go SQLite DB (no CGO). Adjust DSN or driver for your production DB.
    db, err = sql.Open("sqlite", "file:subscriptions.db?_busy_timeout=5000")
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    defer db.Close()

    // Set reasonable limits and verify connection
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(time.Hour)
    if err := db.Ping(); err != nil {
        log.Fatalf("db ping: %v", err)
    }

    // Ensure table exists (idempotent). Keep schema minimal and secure.
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    _, err = db.ExecContext(ctx, `
        CREATE TABLE IF NOT EXISTS subscriptions (
            email TEXT PRIMARY KEY,
            subscribed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            unsubscribed_at DATETIME
        );
    `)
    cancel()
    if err != nil {
        log.Fatalf("create table: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server: %v", err)
    }
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Allow only GET to reduce attack surface
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and sanitize input
    raw := r.URL.Query().Get("email")
    email := strings.TrimSpace(raw)

    // Basic validation: presence, length and syntax
    if email == "" {
        http.Error(w, "missing required parameter", http.StatusBadRequest)
        return
    }
    if len(email) > 254 {
        http.Error(w, "invalid parameter", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        // Do not echo the provided value back to avoid XSS or information leakage
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Use a short context timeout for DB operations to avoid resource exhaustion
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Use parameterized query to avoid SQL injection. Update unsubscribed_at only if not already set.
    res, err := db.ExecContext(ctx,
        `UPDATE subscriptions
         SET unsubscribed_at = CURRENT_TIMESTAMP
         WHERE email = ? AND unsubscribed_at IS NULL`,
        email,
    )
    if err != nil {
        // Log internal error but don't reveal DB details to the client
        log.Printf("unsubscribe update error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Do not reveal whether the email existed or not — prevents account enumeration
    if _, err := res.RowsAffected(); err != nil {
        // Log but respond generically
        log.Printf("rows affected check error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Respond with a generic, non-revealing confirmation. Avoid reflecting user input.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("If the provided email was subscribed, it has been unsubscribed."))
}
