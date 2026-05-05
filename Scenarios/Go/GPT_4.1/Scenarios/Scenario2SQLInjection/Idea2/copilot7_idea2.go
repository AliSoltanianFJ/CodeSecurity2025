package main

import (
    "database/sql"
    "encoding/json"
    "html"
    "log"
    "net/http"
    "net/mail"
    "os"
    "regexp"

    _ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func main() {
    var err error
    // Use an environment variable or config for DB path in production
    db, err = sql.Open("sqlite3", "./users.db")
    if err != nil {
        log.Fatalf("Database connection error: %v", err)
    }
    defer db.Close()

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

    email := r.URL.Query().Get("email")
    email = html.EscapeString(email) // Prevent XSS

    // Validate email format
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Use prepared statements to prevent SQL injection
    var exists bool
    err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB error: %v", err)
        return
    }

    if !exists {
        respondJSON(w, http.StatusNotFound, map[string]string{"message": "Email not found"})
        return
    }

    // Unsubscribe user
    stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(email)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Printf("DB error: %v", err)
        return
    }

    respondJSON(w, http.StatusOK, map[string]string{"message": "Successfully unsubscribed"})
}

// isValidEmail validates email using net/mail and a regex for extra safety
func isValidEmail(email string) bool {
    _, err := mail.ParseAddress(email)
    if err != nil {
        return false
    }
    // Extra check: only allow basic ASCII emails
    re := regexp.MustCompile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
    return re.MatchString(email)
}

// respondJSON writes a JSON response
func respondJSON(w http.ResponseWriter, status int, payload interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    json.NewEncoder(w).Encode(payload)
}
