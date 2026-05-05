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

    email := r.URL.Query().Get("email")
    email = strings.TrimSpace(email)

    // Validate email format
    if email == "" || !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Open database (use environment variable for DB path in production)
    db, err := sql.Open("sqlite3", "./users.db")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB open error: %v", err)
        return
    }
    defer db.Close()

    // Use prepared statement to prevent SQL injection
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }

    if !exists {
        writeJSON(w, response{Message: "Email not found or already unsubscribed"})
        return
    }

    // Unsubscribe (mark as unsubscribed, don't delete for audit)
    stmt, err := db.Prepare("UPDATE users SET unsubscribed = 1 WHERE email = ?")
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

    writeJSON(w, response{Message: "Successfully unsubscribed"})
}

// isValidEmail validates email using net/mail
func isValidEmail(email string) bool {
    // Prevent buffer overflow by limiting length
    if len(email) > 254 {
        return false
    }
    _, err := mail.ParseAddress(email)
    return err == nil
}

// writeJSON writes a JSON response with proper escaping
func writeJSON(w http.ResponseWriter, resp response) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    enc.Encode(resp)
}
