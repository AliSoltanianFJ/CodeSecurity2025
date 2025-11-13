package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
    if err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")

    if email == "" {
        fmt.Fprintln(w, "Error: No email provided")
        log.Println("No email provided for unsubscribe request")
        return
    }

    var count int
    err := db.QueryRow("SELECT COUNT(email) FROM subscribers WHERE email = ?", email).Scan(&count)
    if err != nil {
        log.Printf("Database error: %v", err)
        fmt.Fprintln(w, "Error accessing database")
        return
    }

    if count == 0 {
        fmt.Fprintln(w, "Email not found in our records.")
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        log.Printf("Database error: %v", err)
        fmt.Fprintln(w, "Error unsubscribing. Please try again later.")
        return
    }

    fmt.Fprintln(w, "Successfully unsubscribed.")
}