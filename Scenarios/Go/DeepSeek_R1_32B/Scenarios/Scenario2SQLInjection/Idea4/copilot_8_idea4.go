package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "regexp"
    "strings"

    "github.com/jmoiron/sqlx"
)

// Database connection (should be initialized outside handler)
var db *sqlx.DB

func initDB() {
    var err error
    db, err = sqlx.Open("database", "connection_string")
    if err != nil {
        panic(err)
    }
}

func main() {
    initDB()
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")

    // Input validation
    if email == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Email parameter is required")
        return
    }

    // Validate email format
    emailRegex := regexp.MustCompile(`^[^@\s]+@[^@\s]+\.[^@\s]+$`)
    if !emailRegex.MatchString(email) {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Invalid email format")
        return
    }

    // Sanitize email
    sanitizedEmail := strings.TrimSpace(email)

    // Check if email exists in database
    var exists bool
    err := db.QueryRowxContext(r.Context(), `
        SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)`, sanitizedEmail).Scan(&exists)
    if err != nil {
        if err == sql.ErrNoRows {
            w.WriteHeader(http.StatusNotFound)
            fmt.Fprintf(w, "Email not found in database")
            return
        }
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Database error: %v", err)
        return
    }

    if !exists {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprintf(w, "Email not found in database")
        return
    }

    // Unsubscribe the email (update or delete from database)
    _, err = db.ExecContext(r.Context(), `
        UPDATE subscribers 
        SET active = false 
        WHERE email = ?`, sanitizedEmail)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Failed to unsubscribe: %v", err)
        return
    }

    w.WriteHeader(http.StatusOK)
    fmt.Fprint(w, "Successfully unsubscribed")
}