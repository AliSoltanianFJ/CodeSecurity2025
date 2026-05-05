// ...existing code...
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

func main() {
    // Use DATABASE_URL environment variable (Postgres DSN). Do not hardcode credentials.
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL must be set")
    }

    db, err := sql.Open("postgres", dsn)
    if err != nil {
        log.Fatal("failed to open database:", err)
    }
    defer db.Close()

    // Verify DB connectivity with a short timeout.
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal("database unreachable:", err)
    }

    http.HandleFunc("/unsubscribe", func(w http.ResponseWriter, r *http.Request) {
        unsubscribe(w, r, db)
    })
    // Listen on a non-privileged port for local development.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request, db *sql.DB) {
    // Only allow GET for this endpoint.
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Ensure response is treated as plain text and avoid reflecting input.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Read and validate the email parameter.
    raw := strings.TrimSpace(r.URL.Query().Get("email"))
    if raw == "" {
        // Do not reveal whether an email is subscribed — return a generic message.
        w.WriteHeader(http.StatusOK)
        _, _ = w.Write([]byte("If the email was subscribed, it has been removed."))
        return
    }

    // Protect against overly long input.
    if len(raw) > 254 {
        http.Error(w, "Invalid email", http.StatusBadRequest)
        return
    }

    // Use the standard library to parse and validate the email format.
    parsed, err := mail.ParseAddress(raw)
    if err != nil || parsed.Address == "" {
        http.Error(w, "Invalid email", http.StatusBadRequest)
        return
    }
    email := strings.ToLower(parsed.Address)

    // Use a short context timeout for DB operations to avoid hanging.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Use a transaction and a parameterized query to prevent SQL injection.
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Delete subscriber by email using a parameter placeholder ($1 for Postgres).
    _, err = tx.ExecContext(ctx, "DELETE FROM subscribers WHERE email = $1", email)
    if err != nil {
        _ = tx.Rollback()
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if err := tx.Commit(); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Always respond with a generic message to avoid exposing whether the address existed.
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("If the email was subscribed, it has been removed."))
}
// ...existing code...
