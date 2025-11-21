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
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(time.Minute * 5)

    if err := db.Ping(); err != nil {
        log.Fatal("db ping error")
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
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

    addr, err := mail.ParseAddress(email)
    if err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    email = addr.Address

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    _, err = db.ExecContext(ctx, "UPDATE subscribers SET subscribed = FALSE WHERE email = $1 AND subscribed = TRUE", email)
    if err != nil {
        log.Printf("unsubscribe error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If the address exists, it has been unsubscribed."))
}
