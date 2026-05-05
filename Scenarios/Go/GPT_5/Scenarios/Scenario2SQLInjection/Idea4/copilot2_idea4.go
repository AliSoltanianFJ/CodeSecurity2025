package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"

    _ "github.com/lib/pq"
)

func main() {
    dsn := os.Getenv("DATABASE_URL")
    if strings.TrimSpace(dsn) == "" {
        log.Fatal("missing DATABASE_URL")
    }

    db, err := sql.Open("postgres", dsn)
    if err != nil {
        log.Fatal("db open failed")
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal("db ping failed")
    }

    http.HandleFunc("/unsubscribe", unsubscribeHandler(db))
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

func unsubscribeHandler(db *sql.DB) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        if r.Method != http.MethodGet {
            http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
            return
        }

        email := strings.TrimSpace(r.URL.Query().Get("email"))
        if email == "" || len(email) > 254 {
            http.Error(w, "invalid request", http.StatusBadRequest)
            return
        }

        parsed, err := mail.ParseAddress(email)
        if err != nil || parsed.Address != email || parsed.Name != "" {
            http.Error(w, "invalid request", http.StatusBadRequest)
            return
        }

        ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
        defer cancel()

        var id int64
        err = db.QueryRowContext(ctx,
            "UPDATE subscriptions SET subscribed = FALSE WHERE email = $1 AND subscribed = TRUE RETURNING id",
            email).Scan(&id)

        if err == sql.ErrNoRows {
            http.Error(w, "not found", http.StatusNotFound)
            return
        }
        if err != nil {
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }

        w.WriteHeader(http.StatusNoContent)
    }
}
