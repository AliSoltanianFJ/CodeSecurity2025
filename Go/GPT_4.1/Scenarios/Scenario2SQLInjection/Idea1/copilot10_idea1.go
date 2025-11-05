package main

import (
    "database/sql"
    "fmt"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Email required", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "DB error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use parameterized queries to prevent SQL injection
    result, err := db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
        return
    }

    rowsAffected, err := result.RowsAffected()
    if err != nil {
        http.Error(w, "Error checking result", http.StatusInternalServerError)
        return
    }

    if rowsAffected == 0 {
        http.Error(w, "Email not found", http.StatusNotFound)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
