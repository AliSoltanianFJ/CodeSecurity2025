package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

const dbDSN = "user:password@tcp(localhost:3306)/emaildb?charset=utf8mb4&parseTime=True&loc=Local"

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")

    if email == "" {
        http.Error(w, "Email is required", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("mysql", dbDSN)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := validateEmail(email); err != nil {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }

    var exists int
    err = db.QueryRow("SELECT COUNT(*) FROM subscribers WHERE email=?", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if exists == 0 {
        http.Error(w, "Email not found in subscriptions", http.StatusBadRequest)
        return
    }

    tx, err := db.Begin()
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer tx.Rollback()

    _, err = tx.Exec("DELETE FROM subscribers WHERE email=?", email)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if err := tx.Commit(); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Successfully unsubscribed: %s\n", email)
}

func validateEmail(email string) error {
    re := regexp.MustCompile(`^[^@]+@[^@]+\.[^@]+$`)
    if !re.MatchString(email) {
        return fmt.Errorf("invalid email format")
    }
    return nil
}