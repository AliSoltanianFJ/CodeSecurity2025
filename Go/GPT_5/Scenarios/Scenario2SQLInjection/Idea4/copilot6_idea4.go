// ...existing code...
package main

import (
    "context"
    "database/sql"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "modernc.org/sqlite"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "file:subscribers.db?_fk=1"
    }
    var err error
    db, err = sql.Open("sqlite", dsn)
    if err != nil {
        // fail fast if DB cannot be opened
        os.Exit(1)
    }
    db.SetMaxOpenConns(4)
    db.SetConnMaxLifetime(time.Minute)

    // ensure minimal schema exists
    _, _ = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (email TEXT PRIMARY KEY, subscribed INTEGER NOT NULL)`)

    http.HandleFunc("/unsubscribe", unsubscribe)
    addr := ":" + os.Getenv("PORT")
    if addr == ":" {
        addr = ":8080"
    }
    if err := http.ListenAndServe(addr, nil); err != nil {
        os.Exit(1)
    }
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if email == "" || len(email) > 254 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var subscribed int
    err := db.QueryRowContext(ctx, "SELECT subscribed FROM subscribers WHERE email = ? LIMIT 1", email).Scan(&subscribed)
    if err == sql.ErrNoRows {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if subscribed == 0 {
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("already unsubscribed"))
        return
    }

    res, err := db.ExecContext(ctx, "UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if n, _ := res.RowsAffected(); n == 0 {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
