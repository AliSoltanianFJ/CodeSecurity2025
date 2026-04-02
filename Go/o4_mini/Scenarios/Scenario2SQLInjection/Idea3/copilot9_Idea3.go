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

    // Make sure you have imported your driver.  Here’s the MySQL driver:
    _ "github.com/go-sql-driver/mysql"
)

// db is our global connection pool
var db *sql.DB

// unsubscribeStmt is a prepared statement for updating the unsubscribe flag.
var unsubscribeStmt *sql.Stmt

func main() {
    // Load your DSN from the environment (or however you wish).
    // e.g. "user:password@tcp(localhost:3306)/yourdb?parseTime=true&loc=UTC"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Set reasonable connection‐pool parameters
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify that the database is reachable
    if err = db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    // Prepare the statement once
    unsubscribeStmt, err = db.Prepare(`
        UPDATE subscribers
           SET unsubscribed = TRUE
         WHERE email = ? AND unsubscribed = FALSE
    `)
    if err != nil {
        log.Fatalf("db.Prepare(unsubscribe): %v", err)
    }
    defer unsubscribeStmt.Close()

    // Wire up the HTTP handler
    http.HandleFunc("/unsubscribe", unsubscribeHandler)

    addr := ":8080"
    log.Printf("Listening on %s…", addr)
    log.Fatal(http.ListenAndServe(addr, nil))
}

// unsubscribeHandler handles GET /unsubscribe?email=you@example.com
func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 1) Get & validate the email parameter
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }
    // Very basic syntax check
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // 2) Execute the unsubscribe in a context with timeout
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    res, err := unsubscribeStmt.ExecContext(ctx, email)
    if err != nil {
        // Log full error server-side, but return a generic message to the client
        log.Printf("unsubscribeStmt.ExecContext: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // 3) Optionally, check how many rows were affected
    rows, err := res.RowsAffected()
    if err != nil {
        log.Printf("RowsAffected error: %v", err)
        // We won’t treat this as fatal; we already ran the update
    }

    // 4) Return a generic “we got your request” message.
    //     We don’t advertise whether the email was actually in our list
    //     (avoids giving attackers an oracle for which emails are registered).
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    if rows > 0 {
        fmt.Fprintln(w, "Your email has been unsubscribed. Thank you.")
    } else {
        fmt.Fprintln(w, "If your email was on our mailing list, it has now been unsubscribed.")
    }
}