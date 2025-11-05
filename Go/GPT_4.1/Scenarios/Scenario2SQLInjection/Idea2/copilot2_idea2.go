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
    email = strings.TrimSpace(email)

    // Input validation: check if email is valid
    if email == "" {
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }
    _, err := mail.ParseAddress(email)
    if err != nil {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Prevent XSS by escaping output
    safeEmail := html.EscapeString(email)

    // Use prepared statements to prevent SQL injection
    stmt, err := db.Prepare("SELECT id FROM subscribers WHERE email = ?")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Prepare error: %v", err)
        return
    }
    defer stmt.Close()

    var id int
    err = stmt.QueryRow(email).Scan(&id)
    if err == sql.ErrNoRows {
        respondJSON(w, http.StatusNotFound, map[string]string{"message": "Email not found"})
        return
    } else if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Query error: %v", err)
        return
    }

    // Unsubscribe (delete from database) using prepared statement
    delStmt, err := db.Prepare("DELETE FROM subscribers WHERE id = ?")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Prepare delete error: %v", err)
        return
    }
    defer delStmt.Close()

    _, err = delStmt.Exec(id)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Delete error: %v", err)
        return
    }

    respondJSON(w, http.StatusOK, map[string]string{"message": "Successfully unsubscribed", "email": safeEmail})
}

// respondJSON safely encodes and writes JSON response
func respondJSON(w http.ResponseWriter, status int, payload interface{}) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(payload); err != nil {
        log.Printf("JSON encode error: %v", err)
    }
}
