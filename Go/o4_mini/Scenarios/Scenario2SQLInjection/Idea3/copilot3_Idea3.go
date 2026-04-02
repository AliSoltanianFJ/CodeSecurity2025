package main

import (
    "context"
    "database/sql"
    "errors"
    "log"
    "net"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    // Load DSN from the environment; e.g.:
    //   export DB_DSN="postgres://user:pass@localhost/dbname?sslmode=disable"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("environment variable DB_DSN is required")
    }

    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // It’s good practice to limit your connection pool:
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)

    // Verify connectivity:
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.PingContext: %v", err)
    }

    mux := http.NewServeMux()
    mux.HandleFunc("/unsubscribe", unsubscribeHandler)

    server := &http.Server{
        Addr:         ":8080",
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
        // You can also set TLSConfig here if you want HTTPS.
    }

    log.Printf("listening on %q...", server.Addr)
    if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// unsubscribeHandler checks for ?email=...,
// validates it, then marks unsubscribed=true in the database.
func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
    // Limit to GET only:
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Derive a context with timeout from the request's context.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Parse & validate email
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }
    if !isValidEmail(email) {
        http.Error(w, "invalid email format", http.StatusBadRequest)
        return
    }

    // Perform the UPDATE:
    result, err := db.ExecContext(ctx,
        `UPDATE subscribers
              SET unsubscribed = TRUE
            WHERE email = $1 AND unsubscribed = FALSE`,
        email,
    )
    if err != nil {
        // Log the error internally, but don't show stack traces to the user
        log.Printf("db.ExecContext error unsubscribing %q: %v", email, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Did we actually update any row?
    rowsAffected, err := result.RowsAffected()
    if err != nil {
        log.Printf("RowsAffected error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if rowsAffected == 0 {
        // Either the email doesn’t exist, or it was already unsubscribed.
        // We return 200 OK with a generic message so attackers can't enumerate
        // your subscriber list. If you really want to expose "not found",
        // you can change this to 404 Not Found.
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If that email exists in our list, it has now been unsubscribed."))
        return
    }

    // Success path
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("You have been unsubscribed. Sorry to see you go!"))
}

// isValidEmail uses net/mail to do a basic sanity check.
// It will reject things that definitely aren't emails,
// but it won’t do 100% RFC compliance (which is usually fine).
func isValidEmail(addr string) bool {
    // net/mail.ParseAddress accepts "Name <user@example.com>",
    // so we make sure it's only the bare address.
    _, err := mail.ParseAddress(addr)
    return err == nil && !containsSpace(addr)
}

// containsSpace is a tiny helper to catch bona fide invalid forms.
// ParseAddress will accept "user @ example . com", so we reject stray whitespace.
func containsSpace(s string) bool {
    for _, r := range s {
        if r == ' ' || r == '\t' || r == '\n' {
            return true
        }
    }
    return false
}