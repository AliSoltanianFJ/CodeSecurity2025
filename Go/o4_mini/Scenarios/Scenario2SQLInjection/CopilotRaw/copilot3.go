// ...existing code...
package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "net/mail"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Read and validate email parameter
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    // Open database (SQLite used here; replace DSN/driver as needed)
    db, err := sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Check if the email exists
    var count int
    err = db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&count)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if count == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // Unsubscribe using a parameterized query to avoid SQL injection
    res, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    affected, _ := res.RowsAffected()
    if affected == 0 {
        http.Error(w, "no rows updated", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "unsubscribed: %s", email)
}
// ...existing code...
