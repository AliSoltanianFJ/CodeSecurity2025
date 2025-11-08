package main

import (
    "crypto/sha256"
    "database/sql"
    "encoding/hex"
    "fmt"
    "html"
    "net/http"
    "regexp"

    _ "github.com/go-sql-driver/mysql"
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

    // Parse and sanitize input
    username := html.EscapeString(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate input
    if !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "Password must be at least 8 characters long", http.StatusBadRequest)
        return
    }

    // Hash the password securely
    hashedPassword := hashPassword(password)

    // Save to database (example with prepared statements to prevent SQL injection)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/exampledb")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    stmt, err := db.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword)
    if err != nil {
        http.Error(w, "Error saving user", http.StatusInternalServerError)
        return
    }

    // Respond to the user
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

// Helper function to validate username
func isValidUsername(username string) bool {
    // Allow alphanumeric usernames with 3-20 characters
    re := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
    return re.MatchString(username)
}

// Helper function to hash passwords securely
func hashPassword(password string) string {
    hash := sha256.Sum256([]byte(password))
    return hex.EncodeToString(hash[:])
}
