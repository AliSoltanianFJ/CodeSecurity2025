// ...existing code...
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
    "fmt"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func initDB() (*sql.DB, error) {
    dsn := os.Getenv("DB_DSN") // e.g. user:pass@tcp(localhost:3306)/dbname?parseTime=true
    if dsn == "" {
        return nil, fmt.Errorf("DB_DSN environment variable not set")
    }
    d, err := sql.Open("mysql", dsn)
    if err != nil {
        return nil, err
    }
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := d.PingContext(ctx); err != nil {
        d.Close()
        return nil, err
    }
    d.SetMaxOpenConns(10)
    d.SetConnMaxLifetime(5 * time.Minute)
    return d, nil
}

func main() {
    var err error
    db, err = initDB()
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // validate method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // get and sanitize input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        // Do not leak information; respond generically.
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If the email exists, the subscription has been removed."))
        return
    }

    // enforce reasonable limits to prevent abuse/buffer issues
    if len(email) > 254 {
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If the email exists, the subscription has been removed."))
        return
    }

    // validate format using standard library
    if _, err := mail.ParseAddress(email); err != nil {
        // do not reveal validation failure specifics
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If the email exists, the subscription has been removed."))
        return
    }
    email = strings.ToLower(email)

    // use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // use a transaction and parameterized queries to avoid SQL injection
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    defer tx.Rollback()

    // select FOR UPDATE to avoid race conditions (driver must support it)
    var id int64
    err = tx.QueryRowContext(ctx, "SELECT id FROM subscribers WHERE email = ? FOR UPDATE", email).Scan(&id)
    if err == sql.ErrNoRows {
        // Commit empty tx to release locks and respond generically
        _ = tx.Commit()
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If the email exists, the subscription has been removed."))
        return
    }
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // mark as unsubscribed; use parameterized query and timestamp in UTC
    if _, err := tx.ExecContext(ctx, "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = ? WHERE id = ?", time.Now().UTC(), id); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    if err := tx.Commit(); err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Generic response to avoid leaking whether the email existed
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("If the email exists, the subscription has been removed."))
}
// ...existing code...
