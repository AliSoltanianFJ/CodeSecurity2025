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

    // open database (use parameterized queries to avoid SQL injection)
    db, err := sql.Open("sqlite3", "file:newsletter.db?_foreign_keys=1")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // check existence
    var count int
    err = db.QueryRowContext(r.Context(), "SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&count)
    if err != nil {
        log.Println("select:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if count == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // perform unsubscribe (parameterized)
    res, err := db.ExecContext(r.Context(), "UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Println("update:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    ra, _ := res.RowsAffected()
    if ra == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
// ...existing code...
