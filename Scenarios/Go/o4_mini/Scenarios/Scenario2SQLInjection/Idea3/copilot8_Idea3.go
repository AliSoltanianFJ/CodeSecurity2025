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

    _ "github.com/go-sql-driver/mysql" // or whatever driver you use
)

var db *sql.DB

func main() {
    // Grab your DSN from an environment variable or other secret store:
    // e.g. "user:password@tcp(host:3306)/dbname?parseTime=true&tls=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Configure your connection pool.
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify that the database is reachable.
    if err := db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    // Create a mux so we can enforce timeouts on ALL handlers.
    mux := http.NewServeMux()
    mux.HandleFunc("/unsubscribe", unsubscribeHandler)

    srv := &http.Server{
        Addr:         ":8080",
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }

    log.Printf("starting server on %s", srv.Addr)
    if err := srv.ListenAndServe(); err != nil {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// unsubscribeHandler checks for ?email=... in the URL, validates it,
// then marks the user as unsubscribed in one single, parameterized UPDATE.
func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET (unsubscribe links are usually GETs)
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 1) Pull out and sanitize the email parameter
    rawEmail := strings.TrimSpace(r.URL.Query().Get("email"))
    if rawEmail == "" {
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }

    // 2) Validate that it is (at least) a syntactically valid address
    if _, err := mail.ParseAddress(rawEmail); err != nil {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }

    // 3) Use a context with timeout so that a slow DB won't hang forever
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // 4) Perform a single UPDATE.  We assume your schema has a nullable
    //    `unsubscribed_at` timestamp column; you could also DELETE, but
    //    logical deletes are often preferable for auditing.
    const stmt = `
        UPDATE subscribers
           SET unsubscribed_at = NOW()
         WHERE email = ?
           AND unsubscribed_at IS NULL;`

    res, err := db.ExecContext(ctx, stmt, rawEmail)
    if err != nil {
        // Log the full error server-side; return a generic 500 to the client.
        log.Printf("unsubscribe UPDATE error for %q: %v", rawEmail, err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    rows, err := res.RowsAffected()
    if err != nil {
        log.Printf("RowsAffected error for %q: %v", rawEmail, err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // 5) If zero rows were affected, either the email wasn't in your table
    //    or it was already unsubscribed.  Either way, you can safely return 200
    //    so that bots or attackers can’t probe who is and isn’t subscribed.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)

    if rows == 0 {
        // Always return 200 for not-found / already-unsubscribed
        w.Write([]byte("If that email was in our system, it is now unsubscribed.\n"))
    } else {
        w.Write([]byte("You have been successfully unsubscribed.\n"))
    }
}