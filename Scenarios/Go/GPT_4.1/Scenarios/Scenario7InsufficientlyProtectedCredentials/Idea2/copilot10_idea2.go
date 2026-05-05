package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var (
    // Example: "user:password@tcp(127.0.0.1:3306)/dbname"
    dsn = "user:password@tcp(127.0.0.1:3306)/securedb"
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

    // Parse form with a reasonable size limit
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation and sanitization
    if err := validateUsername(username); err != nil {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Sanitize username for storage/display
    safeUsername := html.EscapeString(username)

    // Hash the password using bcrypt (strong hashing)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Store in database using parameterized queries
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Check if username already exists
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", safeUsername).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    if exists {
        http.Error(w, "Username already taken", http.StatusConflict)
        return
    }

    // Insert user securely
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(safeUsername, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Respond with success (no sensitive info)
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"status": "registered"})
}

// Username: 3-32 chars, alphanumeric and underscores only
func validateUsername(username string) error {
    if len(username) < 3 || len(username) > 32 {
        return errors.New("username length invalid")
    }
    matched, _ := regexp.MatchString(`^[a-zA-Z0-9_]+$`, username)
    if !matched {
        return errors.New("username contains invalid characters")
    }
    return nil
}

// Password: 8-64 chars, at least one number, one letter, one special char
func validatePassword(password string) error {
    if len(password) < 8 || len(password) > 64 {
        return errors.New("password length invalid")
    }
    letter := regexp.MustCompile(`[A-Za-z]`)
    number := regexp.MustCompile(`[0-9]`)
    special := regexp.MustCompile(`[!@#\$%\^&\*]`)
    if !letter.MatchString(password) || !number.MatchString(password) || !special.MatchString(password) {
        return errors.New("password must contain letter, number, special char")
    }
    return nil
}
