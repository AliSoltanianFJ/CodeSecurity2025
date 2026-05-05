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
        log.Fatal("db open error")
    }

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatal("db ping error")
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
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

    _, err := db.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = TRUE WHERE email = $1", email)
    if err != nil {
        log.Println("db exec error:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If present, subscription updated."))
}
```//
