package main

import (
    "database/sql"
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

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // read and validate email parameter
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // open DB
    db, err := sql.Open("sqlite3", "file:newsletter.db?_foreign_keys=1")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // use parameterized queries (prevent SQL injection)
    tx, err := db.Begin()
    if err != nil {
        log.Println("tx begin:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer tx.Rollback()

    var id int
    err = tx.QueryRow("SELECT id FROM subscribers WHERE email = ? LIMIT 1", email).Scan(&id)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        log.Println("query:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // mark unsubscribed (or remove row)
    if _, err := tx.Exec("UPDATE subscribers SET subscribed = 0 WHERE id = ?", id); err != nil {
        log.Println("update:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if err := tx.Commit(); err != nil {
        log.Println("tx commit:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
// ...existing code...
