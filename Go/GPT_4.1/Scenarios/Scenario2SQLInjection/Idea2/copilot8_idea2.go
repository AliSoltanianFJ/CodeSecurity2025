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
    db, err = sql.Open("sqlite3", "./emails.db")
    if err != nil {
        log.Fatalf("Database connection error: %v", err)
    }
    defer db.Close()

    // Ensure table exists
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT UNIQUE NOT NULL
    )`)
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

    // Limit request body size to prevent buffer overflow
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    // Get and sanitize email parameter
    email := r.URL.Query().Get("email")
    email = strings.TrimSpace(email)
    email = html.EscapeString(email)

    // Validate email format
    _, err := mail.ParseAddress(email)
    if err != nil || len(email) > 254 {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Use prepared statements to prevent SQL injection
    stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Printf("Prepare error: %v", err)
        return
    }
    defer stmt.Close()

    res, err := stmt.Exec(email)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Printf("Exec error: %v", err)
        return
    }

    rowsAffected, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Printf("RowsAffected error: %v", err)
        return
    }

    // Respond with JSON, escaping output to prevent XSS
    w.Header().Set("Content-Type", "application/json")
    response := map[string]string{}
    if rowsAffected > 0 {
        response["message"] = "Successfully unsubscribed."
    } else {
        response["message"] = "Email not found."
    }
    json.NewEncoder(w).Encode(response)
}
