package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "regexp"

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
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }

    // basic email validation to avoid obvious invalid input
    emailRe := regexp.MustCompile(`^[^\s@]+@[^\s@]+\.[^\s@]+$`)
    if !emailRe.MatchString(email) {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a parameterized query to prevent SQL injection
    res, err := db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        log.Printf("db exec error: %v", err)
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        return
    }

    affected, err := res.RowsAffected()
    if err != nil {
        log.Printf("rows affected error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if affected == 0 {
        http.NotFound(w, r)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
// ...existing code...
