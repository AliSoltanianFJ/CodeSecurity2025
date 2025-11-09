package main

import (
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    _ "github.com/mattn/go-sqlite3"
)

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // validate input
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }
    // basic syntactic validation
    addr, err := mail.ParseAddress(email)
    if err != nil || len(addr.Address) > 254 {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    // open database
    db, err := sql.Open("sqlite3", "file:newsletter.db?_foreign_keys=1")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // use parameterized queries to avoid SQL injection
    var count int
    if err := db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", addr.Address).Scan(&count); err != nil {
        log.Println("db query:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if count == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // mark unsubscribed (or DELETE, depending on schema) using parameters
    res, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", addr.Address)
    if err != nil {
        log.Println("db update:", err)
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
