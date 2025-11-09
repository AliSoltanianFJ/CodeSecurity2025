// ...existing code...
package main

import (
    "database/sql"
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

    var count int
    err = db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&count)
    if err != nil {
        log.Println(err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if count == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        log.Println(err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
// ...existing code...
