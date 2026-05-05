// ...existing code...
package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        log.Fatal(err)
    }
    // keep DB open for the lifetime of the server
    // create table and seed a sample row
    _, err = db.Exec(`
    CREATE TABLE IF NOT EXISTS subscribers (
        email TEXT PRIMARY KEY,
        unsubscribed INTEGER NOT NULL DEFAULT 0
    );
    INSERT OR IGNORE INTO subscribers(email) VALUES ('alice@example.com');
    `)
    if err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    //-copilot next line-
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    // Use parameterized queries to avoid SQL injection.
    var unsub int
    err := db.QueryRowContext(r.Context(), "SELECT unsubscribed FROM subscribers WHERE email = ?", email).Scan(&unsub)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        log.Println("query error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if unsub == 1 {
        fmt.Fprintf(w, "email %s is already unsubscribed\n", email)
        return
    }

    res, err := db.ExecContext(context.Background(), "UPDATE subscribers SET unsubscribed = 1 WHERE email = ?", email)
    if err != nil {
        log.Println("update error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    affected, _ := res.RowsAffected()
    if affected == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    fmt.Fprintf(w, "email %s unsubscribed\n", email)
}
// ...existing code...
