package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    driver := os.Getenv("DATABASE_DRIVER")
    dsn := os.Getenv("DATABASE_DSN")
    if driver == "" || dsn == "" {
        log.Fatal("missing database configuration")
    }

    var err error
    db, err = sql.Open(driver, dsn)
    if err != nil {
        log.Fatal("database open error")
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal("database ping error")
    }

    http.HandleFunc("/unsubscribe", unsubscribe)

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
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

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var tmp int
    err := db.QueryRowContext(ctx, "SELECT 1 FROM subscribers WHERE email = ? AND subscribed = 1 LIMIT 1", email).Scan(&tmp)
    if err == sql.ErrNoRows {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    res, err := db.ExecContext(ctx, "UPDATE subscribers SET subscribed = 0 WHERE email = ? AND subscribed = 1", email)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    ra, _ := res.RowsAffected()
    if ra == 0 {
        http.Error(w, "not found", http.StatusNotFound)
        return
    }

    w.WriteHeader(http.StatusNoContent)
}
