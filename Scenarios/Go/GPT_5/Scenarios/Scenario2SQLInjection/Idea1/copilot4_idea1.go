package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "net/mail"
    "strings"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" {
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }

    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    res, err := db.ExecContext(r.Context(), "DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    affected, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    if affected == 0 {
        http.NotFound(w, r)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
```// filepath:
