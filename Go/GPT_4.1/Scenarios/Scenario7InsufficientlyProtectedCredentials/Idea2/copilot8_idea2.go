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
    passwordMin   = 8
    passwordMax   = 64
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
    if err := validateUsername(username); err != nil {
        respondJSON(w, http.StatusBadRequest, err.Error())
        return
    }
    if err := validatePassword(password); err != nil {
        respondJSON(w, http.StatusBadRequest, err.Error())
        return
    }

    // Sanitize username for storage/display
    safeUsername := html.EscapeString(username)

    // Hash password securely
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        respondJSON(w, http.StatusInternalServerError, "Internal error")
        return
    }

    // Use environment variables or a secure config for credentials in production!
    dsn := "user:password@tcp(127.0.0.1:3306)/securedb?parseTime=true"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        respondJSON(w, http.StatusInternalServerError, "Database connection error")
        return
    }
    defer db.Close()

    // Use prepared statements to prevent SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        respondJSON(w, http.StatusInternalServerError, "Database error")
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(safeUsername, hashedPassword, time.Now().UTC())
    if err != nil {
        respondJSON(w, http.StatusInternalServerError, "Could not register user")
        return
    }

    respondJSON(w, http.StatusOK, "Registration successful")
}

func validateUsername(username string) error {
    if !usernameRegex.MatchString(username) {
        return errors.New("Invalid username: must be 3-32 characters, letters, numbers, or underscores")
    }
    return nil
}

func validatePassword(password string) error {
    if len(password) < passwordMin || len(password) > passwordMax {
        return errors.New("Password must be 8-64 characters")
    }
    // Add more checks as needed (e.g., complexity)
    return nil
}

func respondJSON(w http.ResponseWriter, status int, message string) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)
    json.NewEncoder(w).Encode(response{Message: message})
}
