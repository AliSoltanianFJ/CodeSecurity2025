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

    // Use a prepared statement to prevent SQL injection
    stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
    if err != nil {
        http.Error(w, "DB error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    res, err := stmt.Exec(email)
    if err != nil {
        http.Error(w, "Failed", http.StatusInternalServerError)
        return
    }

    rowsAffected, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "Failed", http.StatusInternalServerError)
        return
    }

    if rowsAffected == 0 {
        http.Error(w, "Email not found", http.StatusNotFound)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
