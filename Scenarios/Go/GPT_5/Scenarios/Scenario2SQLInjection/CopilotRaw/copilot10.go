// ...existing code...
package main

import (
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    // ensure table exists
    _, err = db.Exec(`
    CREATE TABLE IF NOT EXISTS subscribers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT UNIQUE NOT NULL,
        unsubscribed INTEGER NOT NULL DEFAULT 0
    );`)
    if err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("Listening on :8080")
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

    // validate email format
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    // Check if the email exists and whether it's already unsubscribed.
    var unsubscribed int
    err := db.QueryRow("SELECT unsubscribed FROM subscribers WHERE email = ?", email).Scan(&unsubscribed)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Println("db select error:", err)
        return
    }

    if unsubscribed != 0 {
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("already unsubscribed"))
        return
    }

    // Use parameterized query to avoid SQL injection.
    _, err = db.Exec("UPDATE subscribers SET unsubscribed = 1 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        log.Println("db update error:", err)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("successfully unsubscribed"))
}
// ...existing code...
