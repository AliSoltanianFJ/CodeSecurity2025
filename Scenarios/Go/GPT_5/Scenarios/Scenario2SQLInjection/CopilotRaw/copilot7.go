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

func main() {
    var err error
    db, err = sql.Open("sqlite3", "./emails.db")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    // ensure table exists
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        email TEXT PRIMARY KEY,
        subscribed INTEGER DEFAULT 1
    )`)
    if err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // only GET supported here
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // safe: use prepared statements / parameterized queries to avoid injection
    var found string
    err := db.QueryRow("SELECT email FROM subscribers WHERE email = ?", email).Scan(&found)
    if err == sql.ErrNoRows {
        http.NotFound(w, r)
        return
    }
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Println("query error:", err)
        return
    }

    res, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Println("update error:", err)
        return
    }
    ra, _ := res.RowsAffected()
    if ra == 0 {
        // should not normally happen, but handle gracefully
        http.Error(w, "could not unsubscribe", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s\n", email)
}
