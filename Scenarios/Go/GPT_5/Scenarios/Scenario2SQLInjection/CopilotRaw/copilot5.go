package main

// ...existing code...
import (
    "database/sql"
    "fmt"
    "log"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    db, err := sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    if err := ensureTable(db); err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", func(w http.ResponseWriter, r *http.Request) {
        unsubscribe(db, w, r)
    })

    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(db *sql.DB, w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing 'email' parameter", http.StatusBadRequest)
        return
    }

    var subscribed int
    err := db.QueryRow("SELECT subscribed FROM subscribers WHERE email = ?", email).Scan(&subscribed)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        log.Println("query error:", err)
        return
    }

    if subscribed == 0 {
        fmt.Fprintln(w, "already unsubscribed")
        return
    }

    if _, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email); err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        log.Println("update error:", err)
        return
    }

    fmt.Fprintln(w, "unsubscribed")
}

func ensureTable(db *sql.DB) error {
    _, err := db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        email TEXT PRIMARY KEY,
        subscribed INTEGER NOT NULL DEFAULT 1
    )`)
    return err
}
