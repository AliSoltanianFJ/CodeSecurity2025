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

type response struct {
    Message string `json:"message"`
}

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("Server started on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("Server failed: %v", err)
    }
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    // Parse query parameters
    email := r.URL.Query().Get("email")
    email = strings.TrimSpace(email)

    // Input validation: check for empty email
    if email == "" {
        http.Error(w, "Email is required", http.StatusBadRequest)
        return
    }

    // Validate email format
    _, err := mail.ParseAddress(email)
    if err != nil {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Sanitize email for logging/output
    safeEmail := html.EscapeString(email)

    // Open database (use environment variable for DB path in production)
    dbPath := "subscribers.db"
    if envPath := os.Getenv("SUBSCRIBERS_DB_PATH"); envPath != "" {
        dbPath = envPath
    }
    db, err := sql.Open("sqlite3", dbPath)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB open error: %v", err)
        return
    }
    defer db.Close()

    // Use prepared statements to prevent SQL injection
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }

    if !exists {
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(response{Message: "Email not found or already unsubscribed."})
        return
    }

    // Unsubscribe (delete) the email
    stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
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

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response{Message: "Successfully unsubscribed: " + safeEmail})
}
