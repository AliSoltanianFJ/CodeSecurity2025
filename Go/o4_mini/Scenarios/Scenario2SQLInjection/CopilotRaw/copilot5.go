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
    db, err = sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Fatal("failed to open database:", err)
    }
    // ensure table exists
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        email TEXT PRIMARY KEY,
        subscribed INTEGER NOT NULL DEFAULT 1
    )`)
    if err != nil {
        log.Fatal("failed to create table:", err)
    }
}

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // Use parameterized queries to avoid SQL injection
    var subscribed int
    err := db.QueryRow("SELECT subscribed FROM subscribers WHERE email = ?", email).Scan(&subscribed)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        log.Println("query error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if subscribed == 0 {
        fmt.Fprintln(w, "already unsubscribed")
        return
    }

    _, err = db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Println("update error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "unsubscribed")
}
