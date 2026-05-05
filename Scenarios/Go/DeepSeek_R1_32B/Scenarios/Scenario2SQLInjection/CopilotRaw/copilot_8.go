package main

import (
    "database/sql"
    "fmt"
    "io"
    "log"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.FormValue("email")
    if email == "" {
        io.WriteString(w, "Error: Email parameter is missing.")
        return
    }

    db, err := sql.Open("sqlite3", "./subscriber.db")
    if err != nil {
        log.Printf("Failed to open database: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var id int
    err = db.QueryRow("SELECT id FROM subscribers WHERE email = ?", email).Scan(&id)
    if err != nil {
        if err == sql.ErrNoRows {
            io.WriteString(w, "Email not found in database.")
            w.WriteHeader(http.StatusNotFound)
        } else {
            log.Printf("Database error: %v", err)
            w.WriteHeader(http.StatusInternalServerError)
        }
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        log.Printf("Failed to delete record: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    io.WriteString(w, "Successfully unsubscribed.")
    w.WriteHeader(http.StatusOK)
}