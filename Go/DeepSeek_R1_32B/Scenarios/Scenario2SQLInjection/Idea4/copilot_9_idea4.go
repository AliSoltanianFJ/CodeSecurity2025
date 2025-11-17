package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

// UnsubscribeRequest represents the request structure
type UnsubscribeRequest struct {
    Email string `json:"email"`
}

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.FormValue("email")

    if email == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Email is required")
        return
    }

    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
    if err != nil {
        log.Printf("Error connecting to database: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Internal server error")
        return
    }
    defer db.Close()

    query := `SELECT COUNT(*) FROM users WHERE email = ?`
    var count int
    err = db.QueryRow(query, email).Scan(&count)
    if err != nil {
        log.Printf("Database query error: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Internal server error")
        return
    }

    if count == 0 {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprintf(w, "Email not found in database")
        return
    }

    updateQuery := `UPDATE users SET subscribed = false WHERE email = ?`
    result, err := db.Exec(updateQuery, email)
    if err != nil {
        log.Printf("Update error: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Internal server error")
        return
    }

    rowsAffected, err := result.RowsAffected()
    if err != nil || rowsAffected == 0 {
        log.Println("Failed to update subscription status")
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Internal server error")
        return
    }

    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Successfully unsubscribed email: %s", email)
}