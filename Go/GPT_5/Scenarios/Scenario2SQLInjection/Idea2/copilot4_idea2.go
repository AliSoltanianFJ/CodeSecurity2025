package main

import (
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "strings"

    _ "modernc.org/sqlite"
)

// ...existing code...
var db *sql.DB

func main() {
    var err error
    // open a local sqlite database (pure-Go driver). DSN uses file: mode.
    db, err = sql.Open("sqlite", "file:subscribers.db?_foreign_keys=1")
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }
    // keep connections limited for a simple app
    db.SetMaxOpenConns(1)

    // Create table if it doesn't exist. email primary key ensures uniqueness.
    _, err = db.Exec(`
        CREATE TABLE IF NOT EXISTS subscribers (
            email TEXT PRIMARY KEY,
            unsubscribed INTEGER NOT NULL DEFAULT 0
        )
    `)
    if err != nil {
        log.Fatalf("failed to prepare database: %v", err)
    }

    // Ensure DB is closed on process exit.
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint as specified
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Extract and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }
    // Basic size limit to avoid abuse
    if len(email) > 254 {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    // Reject control characters/newlines to avoid header injection or log injection
    if strings.ContainsAny(email, "\r\n") {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // Validate email format using net/mail
    addr, err := mail.ParseAddress(email)
    if err != nil || addr == nil || addr.Address == "" {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }
    normalized := strings.ToLower(strings.TrimSpace(addr.Address))

    // Use prepared statement to prevent SQL injection. Do not reveal whether the email existed.
    tx, err := db.Begin()
    if err != nil {
        log.Printf("db begin error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    stmt, err := tx.Prepare("UPDATE subscribers SET unsubscribed = 1 WHERE email = ?")
    if err != nil {
        tx.Rollback()
        log.Printf("prepare statement error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(normalized)
    if err != nil {
        tx.Rollback()
        log.Printf("exec update error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    // Commit whether or not a row was changed to avoid revealing existence
    if err := tx.Commit(); err != nil {
        log.Printf("tx commit error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Generic response to avoid account enumeration (do NOT echo the provided email).
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If the provided email exists, it has been unsubscribed."))
}
// ...existing code...
