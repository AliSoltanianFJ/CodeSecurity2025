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
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

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
        log.Printf("db open error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a parameterized query to avoid SQL injection
    res, err := db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        log.Printf("db exec error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    rows, err := res.RowsAffected()
    if err != nil {
        log.Printf("rows affected error: %v", err)
    }

    if rows == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
// ...existing code...
