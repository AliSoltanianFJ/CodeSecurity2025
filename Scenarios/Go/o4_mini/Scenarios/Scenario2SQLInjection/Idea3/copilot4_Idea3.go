package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    // Read your DB connection string from the environment
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL environment variable is required")
    }

    // Open the database (Postgres in this example)
    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Optional: tune your connection pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify that the database is reachable
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.PingContext: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)

    addr := ":8080"
    log.Printf("Listening on %s…", addr)
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// unsubscribe handles GET /unsubscribe?email=foo@example.com
// It validates the email, then marks it unsubscribed in the DB.
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Pull the email parameter
    rawEmail := r.URL.Query().Get("email")
    if rawEmail == "" {
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }

    // 3) Validate email format
    addr, err := mail.ParseAddress(rawEmail)
    if err != nil {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }
    email := addr.Address

    // 4) Create a context with timeout for the DB operation
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // 5) Perform a parameterized UPDATE
    //    We only touch rows where unsubscribed=false, so we can tell if we actually did anything.
    res, err := db.ExecContext(ctx, `
        UPDATE subscribers
           SET unsubscribed     = TRUE,
               unsubscribed_at  = NOW()
         WHERE email = $1
           AND unsubscribed = FALSE
    `, email)
    if err != nil {
        // Never expose internal errors to clients
        log.Printf("unsubscribe: ExecContext error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // 6) Check how many rows we affected
    n, err := res.RowsAffected()
    if err != nil {
        log.Printf("unsubscribe: RowsAffected error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // 7) Formulate a user‐friendly response
    switch {
    case n == 0:
        // Either the email doesn't exist or was already unsubscribed.
        // It's up to you whether you reveal that distinction.
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprintln(w, "Email not found or already unsubscribed.")
    default:
        // n == 1: We successfully unsubscribed them.
        w.WriteHeader(http.StatusOK)
        fmt.Fprintln(w, "You have been unsubscribed.")
    }
}