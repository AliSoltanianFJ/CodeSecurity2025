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
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }

    // basic validation of email format
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // check existence using a parameterized query to avoid injection
    var id int
    err = db.QueryRow("SELECT id FROM subscribers WHERE email = ?", email).Scan(&id)
    if err == sql.ErrNoRows {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }
    if err != nil {
        log.Println("query row:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // delete using parameterized Exec
    res, err := db.Exec("DELETE FROM subscribers WHERE id = ?", id)
    if err != nil {
        log.Println("delete:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    rows, _ := res.RowsAffected()
    fmt.Fprintf(w, "Unsubscribed: %s (rows_deleted=%d)", email, rows)
}
