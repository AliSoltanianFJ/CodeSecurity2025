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

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // validate method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // get and validate email
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // open database (creates file if needed)
    db, err := sql.Open("sqlite3", "subscriptions.db")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // ensure table exists
    if _, err := db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        email TEXT PRIMARY KEY,
        subscribed INTEGER NOT NULL DEFAULT 1
    )`); err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    // check presence using a parameterized query to avoid SQL injection
    var exists int
    if err := db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&exists); err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    if exists == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // unsubscribe (use parameterized statement)
    res, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    ra, _ := res.RowsAffected()

    fmt.Fprintf(w, "unsubscribed %s (rows affected: %d)\n", email, ra)
}
// ...existing code...
