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
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        log.Println("DB open error:", err)
        return
    }
    defer db.Close()

    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database query error", http.StatusInternalServerError)
        log.Println("Query error:", err)
        return
    }

    if !exists {
        http.Error(w, "Email not found", http.StatusNotFound)
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
        log.Println("Delete error:", err)
        return
    }

    fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
}
