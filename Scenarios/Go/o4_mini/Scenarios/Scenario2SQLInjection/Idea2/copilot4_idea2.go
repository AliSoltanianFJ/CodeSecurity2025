package main

import (
    "context"
    "database/sql"
    "fmt"
    "html"
    "io"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"

    _ "github.com/lib/pq" // Ensure you have the driver for your database (Postgres in this example)
)

// ...existing code...
var db *sql.DB

func main() {
    // ...existing code...
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL environment variable is required")
    }

    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("cannot open database: %v", err)
    }
    // sensible defaults
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify connection
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("cannot connect to database: %v", err)
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
    // Use a short context timeout to avoid hanging DB calls
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Only allow GET or POST for unsubscribe (explicitly)
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize input
    rawEmail := strings.TrimSpace(r.URL.Query().Get("email"))
    if rawEmail == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // Validate email strictly using net/mail
    parsed, err := mail.ParseAddress(rawEmail)
    if err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    email := strings.ToLower(strings.TrimSpace(parsed.Address))

    // Don't reveal whether an email exists or not — respond the same way to avoid account enumeration
    const userVisibleMessage = "If the email address was subscribed, it has been unsubscribed."

    // Begin a transaction for safe update (and to use parameterized queries)
    tx, err := db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelReadCommitted})
    if err != nil {
        log.Printf("db begin tx error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer tx.Rollback() // safe to call even after Commit

    // Use parameterized queries to avoid SQL injection
    var exists bool
    // Use a database-appropriate placeholder ($1 for Postgres). If you use another DB adjust accordingly.
    err = tx.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = $1)", email).Scan(&exists)
    if err != nil {
        log.Printf("db query error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if exists {
        // Update unsubscribed flag and timestamp safely via parameterized query
        _, err = tx.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = TRUE, unsubscribed_at = NOW() WHERE email = $1", email)
        if err != nil {
            log.Printf("db update error: %v", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }
        if err := tx.Commit(); err != nil {
            log.Printf("db commit error: %v", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }
    } else {
        // Do not reveal that the email does not exist. Still return the same message.
        // No DB changes required.
    }

    // Prevent XSS by not reflecting raw user input; escape any text that might be included.
    escaped := html.EscapeString(userVisibleMessage)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = io.WriteString(w, fmt.Sprintf("%s\n", escaped))
}
