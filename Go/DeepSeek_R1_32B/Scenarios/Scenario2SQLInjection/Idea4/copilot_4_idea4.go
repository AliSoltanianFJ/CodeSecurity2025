package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"
    "regexp"
    "time"

    "github.com/gorilla/mux"
    _ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func initDB() {
    var err error
    db, err = sql.Open("sqlite3", "./subscriber.db")
    if err != nil {
        panic(err)
    }
    sqlStmt := `
    CREATE TABLE IF NOT EXISTS subscribers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT UNIQUE NOT NULL,
        subscription_date TIMESTAMP NOT NULL
    );`
    _, err = db.Exec(sqlStmt)
    if err != nil {
        panic(err)
    }
}

func main() {
    initDB()
    r := mux.NewRouter()
    r.HandleFunc("/unsubscribe", unsubscribe).Methods("GET")
    http.ListenAndServe(":8080", r)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")

    if email == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprint(w, "Email is required.")
        return
    }

    // Validate email format
    matched, err := regexp.MatchString(`^[^\s@]+@[^\s@]+\.[^\s@]+$`, email)
    if err != nil || !matched {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprint(w, "Invalid email format.")
        return
    }

    // Sanitize input to prevent SQL injection
    cleanEmail := sanitizeInput(email)

    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", cleanEmail).Scan(&exists)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprint(w, "Error checking subscription.")
        return
    }

    if !exists {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprint(w, "Email not found in subscriptions.")
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", cleanEmail)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprint(w, "Error unsubscribing.")
        return
    }

    response := map[string]string{
        "status":  "success",
        "message": "Successfully unsubscribed.",
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)
}

func sanitizeInput(input string) string {
    badChars := regexp.MustCompile(`[;'\"]`)
    return badChars.ReplaceAllString(input, "")
}