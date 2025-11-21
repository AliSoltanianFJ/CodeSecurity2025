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
var db *sql.DB

func main() {
    // ...existing code...
    dsn := os.Getenv("DB_DSN") // e.g. "postgres://user:pass@host:port/dbname?sslmode=disable"
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }

    // Set reasonable connection settings
    db.SetConnMaxLifetime(time.Minute * 5)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // Verify DB connectivity with a timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("database ping failed: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Validate HTTP method (GET allowed per requirement, but limit to safe handling)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    const maxEmailLen = 254
    if email == "" {
        http.Error(w, "email is required", http.StatusBadRequest)
        return
    }
    if len(email) > maxEmailLen {
        http.Error(w, "email too long", http.StatusBadRequest)
        return
    }

    // Validate email format using net/mail (robust and avoids regex pitfalls)
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Parameterized query prevents SQL injection
    // Do not reveal whether the email existed to the client to avoid privacy leaks.
    // We perform the unsubscribe action if present, but always return a generic response.
    query := `UPDATE subscribers SET unsubscribed = TRUE, updated_at = NOW() WHERE email = $1`
    result, err := db.ExecContext(ctx, query, email)
    if err != nil {
        // Log detailed error server-side but return generic message to client
        log.Printf("unsubscribe db error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Check how many rows were affected; log server-side but do not leak to client
    if rows, _ := result.RowsAffected(); rows > 0 {
        log.Printf("email unsubscribed: [REDACTED]") // Do not log the actual email in production; redact if necessary
    } else {
        log.Printf("unsubscribe requested for non-existing email") // generic server-side log
    }

    // Respond with a safe generic message; do not echo user-supplied data
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If the address was subscribed, it has been unsubscribed."))
}
