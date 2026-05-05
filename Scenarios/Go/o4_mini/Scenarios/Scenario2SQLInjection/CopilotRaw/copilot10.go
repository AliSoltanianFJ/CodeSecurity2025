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

func main() {
    var err error
    db, err = sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    // ensure table exists (for demo)
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        id INTEGER PRIMARY KEY,
        email TEXT UNIQUE,
        subscribed INTEGER
    )`)
    if err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // read email from query
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // check existence using a parameterized query (prevents SQL injection)
    var id int
    err := db.QueryRow("SELECT id FROM subscribers WHERE email = ?", email).Scan(&id)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // unsubscribe (mark subscribed = 0)
    res, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE id = ?", id)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    ra, _ := res.RowsAffected()
    fmt.Fprintf(w, "unsubscribed %d record(s)\n", ra)
}
// ...existing code...
