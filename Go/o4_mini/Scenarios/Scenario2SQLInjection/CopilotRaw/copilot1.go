package main

import (
    "database/sql"
    "fmt"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscriptions.db")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var id int
    err = db.QueryRow("SELECT id FROM subscribers WHERE email = ?", email).Scan(&id)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    _, err = db.Exec("UPDATE subscribers SET subscribed = 0 WHERE id = ?", id)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
// ...existing code...
