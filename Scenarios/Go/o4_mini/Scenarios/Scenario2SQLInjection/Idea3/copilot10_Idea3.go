package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq" // import your driver of choice
)

var db *sql.DB

func main() {
    // 1) Read your DSN/connection string from the environment (or a config file)
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL is not set")
    }

    // 2) Open a single *sql.DB for your entire application
    //    It’s concurrency-safe and manages its own connection pool.
    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // 3) Configure the pool to taste
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // 4) Verify connectivity right now
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    // 5) Wire up your handler
    http.HandleFunc("/unsubscribe", unsubscribeHandler)

    // 6) Start your server
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// unsubscribeHandler safely processes ?email=… on GET and marks that
// email as unsubscribed if it exists.  We always return 200 + a
// generic message so that attackers can’t enumerate your list.
func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET (you could also do POST + CSRF token here)
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if email == "" {
        // Missing parameter
        http.Error(w, "email parameter is required", http.StatusBadRequest)
        return
    }

    // Validate syntactic correctness of the email
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    // Perform the unsubscribe under a short context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // IMPORTANT: parameterized query — no string concatenation
    const stmt = `
        UPDATE subscribers
           SET unsubscribed = TRUE,
               unsubscribed_at = NOW()
         WHERE email = $1
    `
    res, err := db.ExecContext(ctx, stmt, email)
    if err != nil {
        // Log the error internally, but don’t reveal details to the user
        log.Printf("unsubscribe UPDATE error for %q: %v", email, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // rowsAffected==0 means “we didn’t find that address”,
    // rowsAffected==1 means “we did.”  We treat them the same.
    if n, err := res.RowsAffected(); err != nil {
        log.Printf("unsubscribe RowsAffected error for %q: %v", email, err)
    } else {
        log.Printf("unsubscribe for %q affected %d row(s)", email, n)
    }

    // Always return the same generic message & 200 OK
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If that email was on our list, it has now been unsubscribed."))
}