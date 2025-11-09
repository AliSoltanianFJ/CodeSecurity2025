package main

import (
    "context"
    "database/sql"
    "html"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"

    // ...existing code...
    _ "github.com/mattn/go-sqlite3" // optional: ensure a driver is available; change driver via env if needed
)

var db *sql.DB

func main() {
    // ...existing code...
    if err := initDB(); err != nil {
        log.Fatalf("failed to initialize db: %v", err)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Validate HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit query parse to avoid huge URL attacks (Go already handles this well for normal usage,
    // but enforce reasonable limits)
    if len(r.URL.RawQuery) > 2048 {
        http.Error(w, "request too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Get and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }
    if len(email) > 254 { // RFC limit
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Validate email using net/mail (robust parsing) and enforce a simple canonical form
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    email = strings.ToLower(email)

    // Use a short timeout for DB operations to avoid resource exhaustion
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Determine placeholder style depending on driver (supports postgres vs others)
    placeholder := "?"
    driver := db.Driver().Open // placeholder to derive driver info is not available; use env
    dbDriver := strings.ToLower(strings.TrimSpace(os.Getenv("DB_DRIVER")))
    if dbDriver == "" {
        // default to sqlite-style placeholders
        dbDriver = "sqlite"
    }
    if strings.Contains(dbDriver, "postgres") || strings.Contains(dbDriver, "pgx") {
        placeholder = "$1"
    }

    // Check for existence and unsubscribe in a parameterized way
    // We intentionally do not reveal whether the email was present to avoid leaking private info.
    // First attempt an update (idempotent): set unsubscribed = true and record a timestamp.
    updateQuery := ""
    var res sql.Result
    var err error
    now := time.Now().UTC()

    if placeholder == "$1" {
        // postgres-style placeholders
        updateQuery = "UPDATE subscribers SET unsubscribed = $1, unsubscribed_at = $2 WHERE lower(email) = lower($3)"
        res, err = db.ExecContext(ctx, updateQuery, true, now, email)
    } else {
        // sqlite/mysql-style placeholders
        updateQuery = "UPDATE subscribers SET unsubscribed = ?, unsubscribed_at = ? WHERE lower(email) = lower(?)"
        res, err = db.ExecContext(ctx, updateQuery, true, now, email)
    }
    if err != nil {
        // Log the detailed error server-side, but return a generic message to the client.
        log.Printf("db update error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Optionally examine rowsAffected (not exposed to user).
    if rowsAffected, _ := res.RowsAffected(); rowsAffected < 1 {
        // No row updated — still respond with the same generic message to avoid account enumeration.
    }

    // Respond with a generic success message. Do not echo the email back to avoid XSS/leakage.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    // Use html.EscapeString only if any user-controlled content were included; we avoid including it.
    _, _ = w.Write([]byte("If this email was subscribed, it has been processed."))
}

// initDB opens a database connection using DB_DRIVER and DB_DSN environment variables.
// For development you can set DB_DRIVER=sqlite3 and DB_DSN=unsubscribe.db
func initDB() error {
    driver := strings.TrimSpace(os.Getenv("DB_DRIVER"))
    dsn := strings.TrimSpace(os.Getenv("DB_DSN"))

    if driver == "" {
        // sensible defaults for local development
        driver = "sqlite3"
    }
    if dsn == "" {
        // sqlite default file
        dsn = "unsubscribe.db"
    }

    var err error
    db, err = sql.Open(driver, dsn)
    if err != nil {
        return err
    }

    // Set conservative connection pool limits
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Ping with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()
    return db.PingContext(ctx)
}
