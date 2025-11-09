package main

import (
    "database/sql"
    "log"
    "net/http"
    "strings"

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
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }
    // basic sanity check
    if !strings.Contains(email, "@") {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "file:newsletter.db?_foreign_keys=1")
    if err != nil {
        log.Println("db open error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var count int
    if err := db.QueryRow("SELECT COUNT(1) FROM subscribers WHERE email = ?", email).Scan(&count); err != nil {
        log.Println("query error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if count == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    res, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Println("update error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if ra, _ := res.RowsAffected(); ra == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
