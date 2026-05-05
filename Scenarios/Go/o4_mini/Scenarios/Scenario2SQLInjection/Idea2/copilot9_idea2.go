package main

import (
    "context"
    "database/sql"
    "html"
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
    db, err = sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    // keep connection pool small for this example
    db.SetMaxOpenConns(3)
    db.SetConnMaxLifetime(time.Minute * 5)

    // ensure table exists (id, email unique, unsubscribed flag)
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        id INTEGER PRIMARY KEY,
        email TEXT NOT NULL UNIQUE,
        unsubscribed INTEGER NOT NULL DEFAULT 0,
        unsubscribed_at DATETIME
    )`)
    if err != nil {
        log.Fatalf("create table: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // only accept GET for this endpoint per scenario, but still validate strictly
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // get and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    // limit length to protect against abuse
    if len(email) == 0 || len(email) > 254 {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    // validate email format
    parsed, err := mail.ParseAddress(email)
    if err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    normalized := strings.ToLower(strings.TrimSpace(parsed.Address))

    // use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    // Use prepared statements with parameterized queries to avoid SQL injection.
    // We do not reveal whether the email existed to prevent account enumeration.
    tx, err := db.BeginTx(ctx, nil)
    if err == nil {
        // try to set unsubscribed flag if the record exists
        stmt, err := tx.PrepareContext(ctx, `UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = CURRENT_TIMESTAMP WHERE email = ? AND unsubscribed = 0`)
        if err == nil {
            _, _ = stmt.ExecContext(ctx, normalized)
            _ = stmt.Close()
        }
        // commit regardless of whether update affected rows
        _ = tx.Commit()
    } else {
        // if begin failed, log only server-side and continue to respond with generic result
        log.Printf("db begin error: %v", err)
    }

    // Generic response: do NOT disclose whether the email existed or was already unsubscribed.
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    msg := "If the address you provided is registered, it has been unsubscribed."
    // escape output to prevent reflected XSS (we do not include user input, but keep it safe)
    _, _ = w.Write([]byte("<!doctype html><html><head><meta charset=\"utf-8\"><title>Unsubscribe</title></head><body><p>" + html.EscapeString(msg) + "</p></body></html>"))
}
