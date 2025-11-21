// ...existing code...
package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "net/mail"

    _ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("sqlite3", "./subscriptions.db")
    if err != nil {
        log.Fatalf("open db: %v", err)
    }
    // will close when program exits
    defer db.Close()

    // create table if it doesn't exist
    _, err = db.Exec(`
        CREATE TABLE IF NOT EXISTS subscribers (
            email TEXT PRIMARY KEY,
            subscribed INTEGER NOT NULL DEFAULT 1
        )
    `)
    if err != nil {
        log.Fatalf("create table: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server: %v", err)
    }
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // only allow GET for this endpoint (per spec)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // basic email validation
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    // check if the email exists
    var exists int
    err := db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&exists)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Printf("query row: %v", err)
        return
    }

    if exists == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // unsubscribe using a parameterized query to avoid SQL injection
    res, err := db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Printf("delete exec: %v", err)
        return
    }

    rows, _ := res.RowsAffected()
    if rows == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintf(w, "unsubscribed: %s\n", email)
}
