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

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    defer db.Close()

    db.SetMaxOpenConns(10)
    db.SetConnMaxIdleTime(5 * time.Minute)

    // Ensure the table exists (safe schema migration)
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    _, err = db.ExecContext(ctx, `CREATE TABLE IF NOT EXISTS subscribers (email TEXT PRIMARY KEY, unsubscribed INTEGER DEFAULT 0)`)
    cancel()
    if err != nil {
        log.Printf("failed to ensure table: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Accept only GET for this endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize input
    raw := strings.TrimSpace(r.URL.Query().Get("email"))
    if raw == "" {
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }
    if len(raw) > 254 {
        http.Error(w, "Email too long", http.StatusBadRequest)
        return
    }

    // Validate email format using standard library
    if _, err := mail.ParseAddress(raw); err != nil {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Use a transaction and prepared statement to avoid SQL injection
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        log.Printf("tx begin error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer tx.Rollback()

    stmt, err := tx.PrepareContext(ctx, `UPDATE subscribers SET unsubscribed = 1 WHERE email = ?`)
    if err != nil {
        log.Printf("prepare error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    res, err := stmt.ExecContext(ctx, raw)
    if err != nil {
        log.Printf("exec error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Don't reveal whether the email existed — return a generic success message
    if _, err := res.RowsAffected(); err != nil {
        log.Printf("rows affected error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if err := tx.Commit(); err != nil {
        log.Printf("tx commit error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("If the address was registered, it has been unsubscribed."))
}
// ...existing code...
