package main

import (
    "database/sql"
    "encoding/json"
    "html"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"

    _ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func main() {
    var err error
    // Use a file-based SQLite DB for demonstration; in production, use a secure DB and credentials
    db, err = sql.Open("sqlite3", "./users.db")
    if err != nil {
        log.Fatalf("Database connection error: %v", err)
    }
    defer db.Close()

    // Ensure the table exists (for demonstration)
    createTable := `
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT UNIQUE NOT NULL,
        subscribed BOOLEAN NOT NULL DEFAULT 1
    );`
    if _, err := db.Exec(createTable); err != nil {
        log.Fatalf("Failed to create table: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("Server started on :8080")
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and sanitize email parameter
    email := r.URL.Query().Get("email")
    email = strings.TrimSpace(email)
    email = html.EscapeString(email) // Prevent XSS in reflected output

    // Validate email format
    if _, err := mail.ParseAddress(email); err != nil || len(email) > 254 {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Use prepared statements to prevent SQL injection
    var exists bool
    err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE email = ? AND subscribed = 1)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB error: %v", err)
        return
    }

    if !exists {
        http.Error(w, "Email not found or already unsubscribed", http.StatusNotFound)
        return
    }

    // Unsubscribe the user
    stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB prepare error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(email)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB exec error: %v", err)
        return
    }

    // Respond with JSON (avoid leaking private info)
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{
        "message": "Successfully unsubscribed.",
    })
}
