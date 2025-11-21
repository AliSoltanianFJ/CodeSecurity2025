package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        panic("missing DATABASE_DSN")
    }

    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        panic(err)
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        panic(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        panic(err)
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

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var id int64
    err := db.QueryRowContext(ctx,
        `UPDATE subscribers SET subscribed = false WHERE email = $1 AND subscribed = true RETURNING id`,
        email,
    ).Scan(&id)

    w.Header().Set("Content-Type", "application/json")
    if err == sql.ErrNoRows {
        json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
        return
    }
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    json.NewEncoder(w).Encode(map[string]string{"status": "unsubscribed"})
}
