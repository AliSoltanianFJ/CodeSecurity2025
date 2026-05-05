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
    // Only allow POST
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
        writeJSON(w, http.StatusBadRequest, response{Message: html.EscapeString(err.Error())})
        return
    }
    if err := validatePassword(password); err != nil {
        writeJSON(w, http.StatusBadRequest, response{Message: html.EscapeString(err.Error())})
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

    // Prevent SQL injection with prepared statements
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Username may already exist or database error", http.StatusConflict)
        return
    }

    writeJSON(w, http.StatusCreated, response{Message: "Registration successful"})
}

func validateUsername(username string) error {
    if !usernameRegex.MatchString(username) {
        return errors.New("Username must be 3-32 characters, alphanumeric or underscore")
    }
    return nil
}

func validatePassword(password string) error {
    if len(password) < passwordMin || len(password) > passwordMax {
        return errors.New("Password must be between 8 and 64 characters")
    }
    // Add more password strength checks as needed
    return nil
}

func writeJSON(w http.ResponseWriter, status int, resp response) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(resp)
}
