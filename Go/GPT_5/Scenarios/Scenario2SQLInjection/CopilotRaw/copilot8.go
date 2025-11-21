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
    db, err = sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    // ensure table exists
    _, err = db.Exec(`
    CREATE TABLE IF NOT EXISTS subscribers (
        id INTEGER PRIMARY KEY,
        email TEXT UNIQUE NOT NULL,
        unsubscribed INTEGER DEFAULT 0
    )`)
    if err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("Listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // only allow GET for this endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // basic validation to avoid obviously malformed input
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    // use a prepared statement / parameterized query to avoid SQL injection
    res, err := db.Exec("UPDATE subscribers SET unsubscribed = 1 WHERE email = ?", email)
    if err != nil {
        log.Println("db update error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    ra, err := res.RowsAffected()
    if err != nil {
        log.Println("rows affected error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if ra == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    fmt.Fprintf(w, "Email %s has been unsubscribed", email)
}
// ...existing code...
