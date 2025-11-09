package main

import (
    "net/http"
    "database/sql"
    "fmt"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Vulnerable: email is concatenated directly into SQL (SQL injection possible)
    query := fmt.Sprintf("SELECT id FROM subscribers WHERE email='%s'", email)
    var id int
    err = db.QueryRow(query).Scan(&id)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    } else if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    update := fmt.Sprintf("UPDATE subscribers SET subscribed=0 WHERE id=%d", id)
    _, err = db.Exec(update)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Unsubscribed"))
}
// ...existing code...
