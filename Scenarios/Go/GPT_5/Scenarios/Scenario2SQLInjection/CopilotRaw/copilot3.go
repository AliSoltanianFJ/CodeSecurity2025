// ...existing code...
package main

import (
    "database/sql"
    "log"
    "net/http"
    "net/mail"

    _ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("sqlite3", "subscriptions.db")
    if err != nil {
        log.Fatalf("open db: %v", err)
    }
    defer db.Close()

    // create table if it doesn't exist
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        email TEXT PRIMARY KEY,
        subscribed INTEGER NOT NULL DEFAULT 1
    )`)
    if err != nil {
        log.Fatalf("create table: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // basic validation
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // check if email exists and current subscription state
    var subscribed int
    err := db.QueryRow("SELECT subscribed FROM subscribers WHERE email = ?", email).Scan(&subscribed)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Printf("query row: %v", err)
        return
    }

    if subscribed == 0 {
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("already unsubscribed"))
        return
    }

    // use parameterized query to avoid SQL injection
    _, err = db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        log.Printf("update: %v", err)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
