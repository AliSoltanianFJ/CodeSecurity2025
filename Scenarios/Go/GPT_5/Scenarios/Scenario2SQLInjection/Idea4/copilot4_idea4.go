// ...existing code...
package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("missing DATABASE_URL")
    }

    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatal("database connection failed")
    }
    db.SetMaxOpenConns(10)
    db.SetConnMaxIdleTime(5 * time.Minute)
    db.SetConnMaxLifetime(30 * time.Minute)

    http.HandleFunc("/unsubscribe", unsubscribe)

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    log.Fatal(srv.ListenAndServe())
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if email == "" || len(email) > 254 {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    var exists bool
    if err := db.QueryRowContext(ctx, "SELECT EXISTS (SELECT 1 FROM subscribers WHERE email = $1)", email).Scan(&exists); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if !exists {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    if _, err := db.ExecContext(ctx, "UPDATE subscribers SET subscribed = false WHERE email = $1", email); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
}