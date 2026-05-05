package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq" // replace with your driver if you use MySQL, etc.
)

func main() {
    // Load your connection string from an environment variable.
    // e.g. export DATABASE_URL="postgres://user:pass@host/dbname?sslmode=require"
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL environment variable is not set")
    }

    // Open a database handle.
    db, err := sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("unable to open database: %v", err)
    }
    defer db.Close()

    // Optional tuning of the connection pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    http.HandleFunc("/unsubscribe", unsubscribeHandler(db))

    log.Println("Listening on :8080 …")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

// unsubscribeHandler returns an http.HandlerFunc that will unsubscribe the
// given email if it exists and is still subscribed.
func unsubscribeHandler(db *sql.DB) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // 1) Only allow GET (or switch to POST if you prefer)
        if r.Method != http.MethodGet {
            http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
            return
        }

        // 2) Extract and validate the email parameter
        email := r.URL.Query().Get("email")
        if email == "" {
            http.Error(w, "missing email parameter", http.StatusBadRequest)
            return
        }
        if _, err := mail.ParseAddress(email); err != nil {
            http.Error(w, "invalid email address", http.StatusBadRequest)
            return
        }

        // 3) Create a request-scoped context with timeout
        ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
        defer cancel()

        // 4) Try to flip the flag from subscribed → unsubscribed
        res, err := db.ExecContext(ctx,
            `UPDATE subscribers
               SET unsubscribed = TRUE,
                   unsubscribed_at = NOW()
             WHERE email = $1
               AND unsubscribed = FALSE`,
            email,
        )
        if err != nil {
            log.Printf("db update error")
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }

        // 5) Check how many rows were affected
        n, err := res.RowsAffected()
        if err != nil {
            log.Printf("RowsAffected error: %v", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }

        switch {
        case n > 0:
            // successfully unsubscribed
            w.WriteHeader(http.StatusOK)
            w.Write([]byte("unsubscribed successfully"))
            return

        case n == 0:
            // Either the email didn't exist, or it was already unsubscribed.
            var exists bool
            err := db.QueryRowContext(ctx,
                `SELECT EXISTS(
                   SELECT 1 FROM subscribers WHERE email = $1
                 )`, email,
            ).Scan(&exists)
            if err != nil {
                log.Printf("db existence check error")
                http.Error(w, "internal server error", http.StatusInternalServerError)
                return
            }
            if !exists {
                http.Error(w, "email not found", http.StatusNotFound)
                return
            }
            // it existed but was already unsubscribed
            w.WriteHeader(http.StatusOK)
            w.Write([]byte("email was already unsubscribed"))
            return
        }
    }
}