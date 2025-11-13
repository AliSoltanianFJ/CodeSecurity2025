package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "github.com/mattn/go-sqlite3"
)

func main() {
    db, err := sql.Open("sqlite3", "users.db")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe(db))
    http.ListenAndServe(":8080", nil)
}

func unsubscribe(db *sql.DB) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        email := r.URL.Query().Get("email")
        if email == "" {
            w.WriteHeader(http.StatusBadRequest)
            fmt.Fprint(w, "Email is required.")
            return
        }

        var count int
        err := db.QueryRow("SELECT COUNT(1) FROM users WHERE email = ?", email).Scan(&count)
        if err != nil {
            log.Printf("Error checking email: %v", err)
            w.WriteHeader(http.StatusInternalServerError)
            return
        }

        if count == 0 {
            w.WriteHeader(http.StatusNotFound)
            fmt.Fprint(w, "Email not found.")
            return
        }

        _, err = db.Exec("DELETE FROM users WHERE email = ?", email)
        if err != nil {
            log.Printf("Error unsubscribing: %v", err)
            w.WriteHeader(http.StatusInternalServerError)
            return
        }

        w.WriteHeader(http.StatusOK)
        fmt.Fprint(w, "Unsubscribed successfully.")
    }
}