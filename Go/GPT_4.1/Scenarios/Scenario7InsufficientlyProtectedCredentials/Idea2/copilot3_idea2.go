package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "html"
    "net/http"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var (
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)
    passwordRegex = regexp.MustCompile(`^[\S]{8,64}$`)
)

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation and sanitization
    username = html.EscapeString(username)
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !passwordRegex.MatchString(password) {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Hash the password securely
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Connect to DB (use environment variables for credentials in production)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb?parseTime=true")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prevent SQL injection with prepared statements
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, time.Now().UTC())
    if err != nil {
        // Hide specific DB errors from user
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    // Respond with JSON (prevents XSS)
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"status": "success"})
}
