package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "file:newsletter.db?_foreign_keys=1")
    if err != nil {
        log.Println(err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // WARNING: The following queries are constructed from user input and are vulnerable
    // to SQL injection. This mirrors the insecure scenario. Use parameterized queries
    // (db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", email)) in real code.

    var count int
    row := db.QueryRow(fmt.Sprintf("SELECT COUNT(1) FROM subscribers WHERE email='%s'", email))
    if err := row.Scan(&count); err != nil {
        log.Println(err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if count == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    _, err = db.Exec(fmt.Sprintf("DELETE FROM subscribers WHERE email='%s'", email))
    if err != nil {
        log.Println(err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
// ...existing code...
