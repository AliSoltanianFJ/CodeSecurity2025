package main

import (
    "database/sql"
    "encoding/json"
    "html"
    "log"
    "net/http"
    "regexp"

    _ "github.com/mattn/go-sqlite3"
)

var (
    db            *sql.DB
    emailRegex, _ = regexp.Compile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
)

func main() {
    var err error
    db, err = sql.Open("sqlite3", "./users.db")
    if err != nil {
        log.Fatalf("Database connection error: %v", err)
    }
    defer db.Close()

    // Ensure table exists (for demonstration)
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS users (email TEXT PRIMARY KEY, subscribed INTEGER)`)
    if err != nil {
        log.Fatalf("Table creation error: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("Server running on :8080")
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    email = html.EscapeString(email) // Prevent XSS

    // Validate email format
    if !emailRegex.MatchString(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Use prepared statements to prevent SQL injection
    var exists int
    err := db.QueryRow("SELECT COUNT(1) FROM users WHERE email = ?", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    if exists == 0 {
        http.Error(w, "Email not found", http.StatusNotFound)
        return
    }

    // Unsubscribe user
    _, err = db.Exec("UPDATE users SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
        return
    }

    // Respond with JSON (safe, no private info)
    resp := map[string]string{"message": "Successfully unsubscribed"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}
