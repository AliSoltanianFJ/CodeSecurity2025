package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "net/mail"
    "context"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    // open DB once and reuse
    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Fatalf("failed to open DB: %v", err)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", func(w http.ResponseWriter, r *http.Request) {
        unsubscribe(w, r, db)
    })
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request, db *sql.DB) {
    // require GET param "email"
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "email query parameter required", http.StatusBadRequest)
        return
    }

    // basic validation using net/mail
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email address", http.StatusBadRequest)
        return
    }

    ctx := r.Context()

    // Use a prepared statement with parameter binding to avoid SQL injection.
    stmt, err := db.PrepareContext(ctx, "DELETE FROM subscribers WHERE email = ?")
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        log.Printf("prepare failed: %v", err)
        return
    }
    defer stmt.Close()

    res, err := stmt.ExecContext(ctx, email)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        log.Printf("exec failed: %v", err)
        return
    }

    rows, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        log.Printf("RowsAffected failed: %v", err)
        return
    }

    if rows == 0 {
        http.Error(w, fmt.Sprintf("email not found: %s", email), http.StatusNotFound)
        return
    }

    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
