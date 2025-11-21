package main

import (
    "fmt"
    "database/sql"
    _ "github.com/mattn/go-sqlite3"
    "net/http"
    "net/mail"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    res, err := db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        return
    }
    affected, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "failed to confirm", http.StatusInternalServerError)
        return
    }
    if affected == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
