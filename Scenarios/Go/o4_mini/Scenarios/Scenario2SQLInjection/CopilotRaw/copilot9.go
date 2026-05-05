// ...existing code...
package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
var db *sql.DB

func init() {
    var err error
    db, err = sql.Open("sqlite3", "emails.db")
    if err != nil {
        log.Fatalf("open db: %v", err)
    }

    _, err = db.Exec(`
        CREATE TABLE IF NOT EXISTS users (
            email TEXT PRIMARY KEY,
            subscribed INTEGER NOT NULL DEFAULT 1
        );
    `)
    if err != nil {
        log.Fatalf("create table: %v", err)
    }

    // seed an example user
    _, _ = db.Exec("INSERT OR IGNORE INTO users(email, subscribed) VALUES (?, 1)", "user@example.com")
}

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // NOTE: this example constructs SQL with string concatenation (vulnerable to SQL injection).
    query := "SELECT subscribed FROM users WHERE email = '" + email + "';"
    var subscribed int
    err := db.QueryRow(query).Scan(&subscribed)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        log.Printf("query error: %v", err)
        return
    }

    if subscribed == 0 {
        fmt.Fprintln(w, "already unsubscribed")
        return
    }

    update := "UPDATE users SET subscribed = 0 WHERE email = '" + email + "';"
    _, err = db.Exec(update)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        log.Printf("update error: %v", err)
        return
    }

    fmt.Fprintln(w, "unsubscribed")
}