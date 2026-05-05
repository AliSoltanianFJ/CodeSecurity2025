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
    // Example: "user:password@tcp(localhost:3306)/dbname"
    dsn = "user:password@tcp(localhost:3306)/securedb"
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

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    // Parse form data safely
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

    // Hash the password using bcrypt (strong, salted)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Store in database using prepared statements
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    stmt, err := db.Prepare("INSERT INTO users (username, password, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(safeUsername, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Username may already exist", http.StatusConflict)
        return
    }

    // Respond with JSON (safe, no sensitive info)
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

// Password: at least 8 chars, at least one number, one letter
func validatePassword(password string) error {
    if len(password) < 8 || len(password) > 64 {
        return errors.New("password length invalid")
    }
    letter := regexp.MustCompile(`[A-Za-z]`)
    number := regexp.MustCompile(`[0-9]`)
    if !letter.MatchString(password) || !number.MatchString(password) {
        return errors.New("password must contain letters and numbers")
    }
    return nil
}
