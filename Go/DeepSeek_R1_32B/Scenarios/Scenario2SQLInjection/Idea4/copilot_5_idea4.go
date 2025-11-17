package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(dbhost:3306)/dbname")
    if err != nil {
        panic(err)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")

    if email == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprint(w, "Email is required")
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel

    var exists bool
    err := db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Error checking subscription: %v", err)
        return
    }

    if !exists {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprint(w, "Email not found in subscriptions")
        return
    }

    _, err = db.ExecContext(ctx, "DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Error unsubscribing: %v", err)
        return
    }

    response := struct {
        Status  string
        Message string
    }{
        Status:  "success",
        Message: "Successfully unsubscribed",
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)

    logger.Printf("User %s unsubscribed successfully", email)
}