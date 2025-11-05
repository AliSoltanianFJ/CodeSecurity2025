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
    db *sql.DB
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)
)

func main() {
    var err error
    // Use environment variables or a secure config for credentials in production
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb?parseTime=true")
    if err != nil {
        panic("Database connection failed")
    }
    defer db.Close()

    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

type response struct {
    Message string `json:"message"`
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation
    if err := validateCredentials(username, password); err != nil {
        http.Error(w, html.EscapeString(err.Error()), http.StatusBadRequest)
        return
    }

    // Sanitize input (username)
    safeUsername := html.EscapeString(username)

    // Hash the password using bcrypt
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Store user securely using prepared statements
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(safeUsername, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Username already exists or database error", http.StatusConflict)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response{Message: "Registration successful"})
}

func validateCredentials(username, password string) error {
    if !usernameRegex.MatchString(username) {
        return errors.New("Invalid username: must be 3-32 characters, alphanumeric or underscore")
    }
    if len(password) < 8 || len(password) > 64 {
        return errors.New("Password must be 8-64 characters")
    }
    // Add more password checks as needed (e.g., complexity)
    return nil
}
