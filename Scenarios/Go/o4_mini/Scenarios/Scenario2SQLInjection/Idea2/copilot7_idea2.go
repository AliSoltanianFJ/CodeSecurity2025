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

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
var db *sql.DB

func main() {
    // open (or create) a small local sqlite DB; replace with your production DB & driver as needed
    var err error
    db, err = sql.Open("sqlite3", "file:subscribers.db?_busy_timeout=5000&_foreign_keys=1")
    if err != nil {
        log.Fatal("db open:", err)
    }
    // set sensible limits
    db.SetConnMaxLifetime(time.Minute * 3)
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)

    // initialize table if missing (safe; uses a constant SQL statement)
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    _, err = db.ExecContext(ctx, `CREATE TABLE IF NOT EXISTS subscribers (email TEXT PRIMARY KEY, subscribed_at DATETIME)`)
    if err != nil {
        log.Fatal("db init:", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // only allow GET for this endpoint per the scenario description
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // parse and validate input
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        // generic response to avoid revealing details
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }
    // enforce a sane maximum length to avoid abuse
    if len(email) > 254 {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }
    // validate email format using net/mail (robust and standard)
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    // contextual DB operation with timeout to avoid hanging resources
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // use parameterized query (prepared statement) to prevent SQL injection
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        // don't reveal internal error details to client
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        log.Println("begin tx:", err)
        return
    }
    stmt, err := tx.PrepareContext(ctx, `DELETE FROM subscribers WHERE email = ?`)
    if err != nil {
        tx.Rollback()
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        log.Println("prepare:", err)
        return
    }
    defer stmt.Close()

    res, err := stmt.ExecContext(ctx, strings.ToLower(email))
    if err != nil {
        tx.Rollback()
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        log.Println("exec:", err)
        return
    }
    if err := tx.Commit(); err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        log.Println("commit:", err)
        return
    }

    // Do not reveal whether the address existed; provide a generic confirmation to avoid account enumeration
    rowsAffected, _ := res.RowsAffected()
    if rowsAffected > 0 {
        // record minimal server-side log (do not log full email in production)
        log.Printf("unsubscribe request processed")
    } else {
        // still respond with the same generic message
        log.Printf("unsubscribe request processed")
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("If the address was subscribed, it has been unsubscribed."))
}
// ...existing code...
