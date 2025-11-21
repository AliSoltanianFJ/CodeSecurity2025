package main

import (
    "context"
    "crypto/sha256"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"
)

// ...existing code...
func main() {
    // ...existing code...

    // Open database using driver and DSN provided via environment variables.
    // Set DB_DRIVER and DB_DSN before running (e.g. DB_DRIVER=mysql DB_DSN="user:pass@tcp(127.0.0.1:3306)/dbname")
    driver := strings.TrimSpace(os.Getenv("DB_DRIVER"))
    dsn := strings.TrimSpace(os.Getenv("DB_DSN"))
    if driver == "" || dsn == "" {
        log.Fatal("DB_DRIVER and DB_DSN environment variables must be set")
    }

    db, err := sql.Open(driver, dsn)
    if err != nil {
        log.Fatalf("failed to open database connection: %v", err)
    }
    defer db.Close()

    // verify connectivity with a timeout
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("database ping failed: %v", err)
    }

    // register handler with DB closure
    http.HandleFunc("/unsubscribe", func(w http.ResponseWriter, r *http.Request) {
        unsubscribe(w, r, db, driver)
    })
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
// changed code
func unsubscribe(w http.ResponseWriter, r *http.Request, db *sql.DB, driver string) {
    // Only allow GET or POST for this endpoint depending on your policy. Here we accept GET as requested.
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and validate email input
    emailRaw := strings.TrimSpace(r.URL.Query().Get("email"))
    if emailRaw == "" {
        // Do not reveal whether an email is required for privacy; return a generic response.
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    // Prevent excessively long input
    if len(emailRaw) > 254 {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    // Validate email format using net/mail
    addr, err := mail.ParseAddress(emailRaw)
    if err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    email := strings.ToLower(strings.TrimSpace(addr.Address))

    // Log only a non-reversible hash of the email to avoid exposing private info in logs
    sum := sha256.Sum256([]byte(email))
    log.Printf("unsubscribe requested, email_hash=%x", sum[:])

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Prepare driver-specific query with parameterized input to prevent SQL injection
    var query string
    switch strings.ToLower(driver) {
    case "postgres", "pgx":
        query = "UPDATE subscribers SET unsubscribed_at = NOW(), active = false WHERE email = $1 AND active = true"
    default:
        // MySQL / SQLite style placeholder
        query = "UPDATE subscribers SET unsubscribed_at = UTC_TIMESTAMP(), active = 0 WHERE email = ? AND active = 1"
    }

    // Execute update with parameter binding — prevents SQL injection
    res, err := db.ExecContext(ctx, query, email)
    if err != nil {
        // Log error without revealing private data
        log.Printf("database update error: %v", err)
        // Return generic message to caller to avoid account enumeration
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Optionally check rows affected but do not reveal whether an email existed
    _, _ = res.RowsAffected()

    // Return a generic safe message; do not echo user input to avoid XSS
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("If the email exists it has been unsubscribed."))
}
// ...existing code...
