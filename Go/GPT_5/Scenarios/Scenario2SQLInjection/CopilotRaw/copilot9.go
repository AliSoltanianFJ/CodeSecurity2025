// ...existing code...
package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // read email from query
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // open (or create) the sqlite DB — adjust DSN/path as needed
    db, err := sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // ensure table exists (simple schema)
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        id INTEGER PRIMARY KEY,
        email TEXT UNIQUE,
        subscribed INTEGER DEFAULT 1
    )`)
    if err != nil {
        log.Printf("ensure table error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // check existence using a parameterized query to avoid SQL injection
    var count int
    err = db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&count)
    if err != nil {
        log.Printf("query error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if count == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // unsubscribe (update safely using parameterized query)
    res, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Printf("update error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    ra, _ := res.RowsAffected()

    fmt.Fprintf(w, "Unsubscribed %s (rows affected: %d)\n", email, ra)
}
// ...existing code...
