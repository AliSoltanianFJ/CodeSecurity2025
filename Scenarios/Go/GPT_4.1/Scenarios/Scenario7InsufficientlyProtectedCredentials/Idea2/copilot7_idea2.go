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

type response struct {
    Message string `json:"message"`
}

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

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
    if err := validateInput(username, password); err != nil {
        http.Error(w, html.EscapeString(err.Error()), http.StatusBadRequest)
        return
    }

    // Hash password securely
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Connect to DB (use environment variables for credentials in production)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb?parseTime=true")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared statement to prevent SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Username may already exist", http.StatusConflict)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response{Message: "Registration successful"})
}

func validateInput(username, password string) error {
    if !usernameRegex.MatchString(username) {
        return errors.New("Invalid username: must be 3-32 characters, alphanumeric or underscore")
    }
    if !passwordRegex.MatchString(password) {
        return errors.New("Invalid password: must be 8-64 non-whitespace characters")
    }
    return nil
}
