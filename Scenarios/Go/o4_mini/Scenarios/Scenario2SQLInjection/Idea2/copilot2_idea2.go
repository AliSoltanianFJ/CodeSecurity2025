package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"
)

var db *sql.DB

func main() {
    // DB driver and DSN are taken from environment for flexibility.
    // Example:
    //   DB_DRIVER=sqlite3 DB_DSN="file:emails.db?_foreign_keys=1" go run .
    driver := os.Getenv("DB_DRIVER")
    if driver == "" {
        driver = "sqlite3"
    }
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "file:emails.db?_foreign_keys=1"
    }

    var err error
    db, err = sql.Open(driver, dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }
    defer db.Close()

    // Tune connection pool and verify connectivity
    db.SetConnMaxLifetime(3 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("database ping failed: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint per original requirement.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Use a short context to avoid long-running DB ops.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Read and sanitise input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "email parameter required", http.StatusBadRequest)
        return
    }

    // Validate email using net/mail to avoid brittle regexes.
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    // Do not log or echo the raw email (avoid exposing PII).
    // Use parameterised queries to prevent SQL injection.
    var subscriberID int64
    // NOTE: placeholder style ("?") depends on the driver. Adjust to "$1" for postgres if needed.
    err := db.QueryRowContext(ctx, "SELECT id FROM subscribers WHERE email = ? LIMIT 1", email).Scan(&subscriberID)
    if errors.Is(err, sql.ErrNoRows) {
        // Generic response to avoid revealing whether an address existed.
        w.WriteHeader(http.StatusOK)
        fmt.Fprintln(w, "If the address was subscribed, it has been unsubscribed.")
        return
    }
    if err != nil {
        log.Println("database error: query subscriber:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Mark as unsubscribed in a parameterised UPDATE. Adjust column names to match your schema.
    res, err := db.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", subscriberID)
    if err != nil {
        log.Println("database error: update unsubscribe:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    affected, _ := res.RowsAffected()
    if affected == 0 {
        // Unexpected: no row updated. Respond generically.
        w.WriteHeader(http.StatusOK)
        fmt.Fprintln(w, "If the address was subscribed, it has been unsubscribed.")
        return
    }

    // Success - keep response generic and do NOT echo PII.
    w.WriteHeader(http.StatusOK)
    fmt.Fprintln(w, "If the address was subscribed, it has been unsubscribed.")
}
