// ...existing code...
package main

import (
    "context"
    "database/sql"
    _ "github.com/mattn/go-sqlite3"
    "html"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"
)

// ...existing code...
var db *sql.DB

func initDB() {
    driver := os.Getenv("DB_DRIVER")
    dsn := os.Getenv("DB_DSN")
    if driver == "" {
        // sensible default for local/testing
        driver = "sqlite3"
        dsn = "./data.db"
    }
    var err error
    db, err = sql.Open(driver, dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }
    // use conservative pool settings
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)
    db.SetConnMaxLifetime(5 * time.Minute)

    // quick ping with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("database ping failed: %v", err)
    }
}

// ...existing code...
func main() {
    initDB()
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only accept GET per requirement; keep response generic and avoid echoing user data
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "email parameter required", http.StatusBadRequest)
        return
    }

    // Validate email format using standard library (prevents injections and malformed input)
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Parameterized query prevents SQL injection. Assume table 'subscriptions' with columns id, email, unsubscribed.
    var id int
    err := db.QueryRowContext(ctx, "SELECT id FROM subscriptions WHERE email = ?", email).Scan(&id)
    if err == sql.ErrNoRows {
        // Respond with the same generic message whether or not the email existed (prevents info leakage)
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.WriteHeader(http.StatusOK)
        _, _ = w.Write([]byte("If that address was subscribed, it has been removed."))
        return
    }
    if err != nil {
        // Do not leak internal errors to the client
        log.Printf("db lookup error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Try to mark as unsubscribed (idempotent). Use parameterized statement to avoid injection.
    res, err := db.ExecContext(ctx, "UPDATE subscriptions SET unsubscribed = 1, unsubscribed_at = CURRENT_TIMESTAMP WHERE id = ?", id)
    if err != nil {
        log.Printf("db update error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    affected, _ := res.RowsAffected()
    if affected == 0 {
        // As a fallback, attempt a delete (also parameterized)
        _, _ = db.ExecContext(ctx, "DELETE FROM subscriptions WHERE id = ?", id)
    }

    // Safe response: never echo the provided email back (prevents XSS and info leakage).
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(html.EscapeString("If that address was subscribed, it has been removed.")))
}