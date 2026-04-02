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

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // 1) Load your DSN from the environment
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    // 2) Open the database (driver-specific – here we use go-sql-driver/mysql)
    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // 3) Tune your connection pool to taste
    db.SetConnMaxIdleTime(5 * time.Minute)
    db.SetConnMaxLifetime(1 * time.Hour)
    db.SetMaxIdleConns(10)
    db.SetMaxOpenConns(25)

    // 4) Verify that the database is reachable
    pingCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(pingCtx); err != nil {
        log.Fatalf("db.PingContext: %v", err)
    }

    // 5) Wire up your handler
    http.HandleFunc("/unsubscribe", unsubscribe)

    log.Println("Listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// unsubscribe reads ?email=..., validates it, then marks it unsubscribed in the DB.
// We always return a generic 200 OK on success so as not to allow address‐harvesting via timing/404s.
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET here; if you prefer POST you can switch this.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 1) Grab and clean the parameter
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "email parameter is required", http.StatusBadRequest)
        return
    }

    // 2) Validate format
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    // 3) Use a short context, derived from the request
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // 4) Execute a parameterized UPDATE
    //    Here we assume your table has columns (email VARCHAR(...), unsubscribed BOOL, unsubscribed_at DATETIME).
    //    You could also DELETE FROM subscribers WHERE email = ? if you truly want to remove the row.
    res, err := db.ExecContext(ctx, `
        UPDATE subscribers
           SET unsubscribed    = TRUE
             , unsubscribed_at = NOW()
         WHERE email = ?
    `, email)
    if err != nil {
        log.Printf("unsubscribe: db.ExecContext: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 5) We do NOT error or 404 if rowsAffected == 0 because we don't want to leak who is/isn't
    //    in our system.  We simply return success, the operation is idempotent.
    if _, err := res.RowsAffected(); err != nil {
        // This is rare, but handle it anyway
        log.Printf("unsubscribe: RowsAffected failure: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 6) Return a generic 200 OK.  You can also choose 204 No Content.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(`{"success":true}`))
}