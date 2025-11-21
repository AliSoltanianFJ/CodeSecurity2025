package main

import (
    "context"
    "database/sql"
    "fmt"
    "net/http"
    "net/mail"
    "time"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }
    // basic validation
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

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // use parameterized query to avoid SQL injection
    res, err := db.ExecContext(ctx, "DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        return
    }

    n, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "failed to determine result", http.StatusInternalServerError)
        return
    }
    if n == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
