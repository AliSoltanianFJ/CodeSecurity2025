package main

import (
    "database/sql"
    "fmt"
    "log"
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
        http.Error(w, "Email parameter missing", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./users.db")
    if err != nil {
        log.Printf("Error opening database: %v\n", err)
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var exists string
    err = db.QueryRow("SELECT email FROM users WHERE email = ?", email).Scan(&exists)
    if err == sql.ErrNoRows {
        fmt.Fprintf(w, "Email %s not found in database", email)
        return
    } else if err != nil {
        log.Printf("Error checking email: %v\n", err)
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    _, err = db.Exec("DELETE FROM users WHERE email = ?", email)
    if err != nil {
        log.Printf("Error unsubscribing email: %v\n", err)
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Successfully unsubscribed email: %s", email)
}