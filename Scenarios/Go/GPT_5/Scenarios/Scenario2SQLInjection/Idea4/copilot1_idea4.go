// ...existing code...
package main

import (
    "context"
    "database/sql"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    dsn := strings.TrimSpace(os.Getenv("DATABASE_URL"))
    if dsn == "" {
        http.ListenAndServe(":8080", nil) // fail closed: start only to return errors instead of leaking env
        return
    }

    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        http.ListenAndServe(":8080", nil)
        return
    }
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(5 * time.Minute)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        http.ListenAndServe(":8080", nil)
        return
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" || len(email) > 254 {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var subscribed bool
    err := db.QueryRowContext(ctx, "SELECT subscribed FROM users WHERE email = $1", email).Scan(&subscribed)
    if err == sql.ErrNoRows {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    if !subscribed {
        w.WriteHeader(http.StatusNoContent)
        return
    }

    res, err := db.ExecContext(ctx, "UPDATE users SET subscribed = FALSE, unsubscribed_at = NOW() WHERE email = $1 AND subscribed = TRUE", email)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    affected, err := res.RowsAffected()
    if err != nil || affected == 0 {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusNoContent)
}
