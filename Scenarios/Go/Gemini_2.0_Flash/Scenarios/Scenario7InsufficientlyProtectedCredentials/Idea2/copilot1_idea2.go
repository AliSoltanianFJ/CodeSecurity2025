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

    // Save to database
    err := saveUserToDatabase(username, hashedPassword)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Respond to the client
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

// Validate username (e.g., alphanumeric, 3-20 characters)
func isValidUsername(username string) bool {
    re := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
    return re.MatchString(username)
}

// Hash the password using SHA-256
func hashPassword(password string) string {
    hash := sha256.Sum256([]byte(password))
    return hex.EncodeToString(hash[:])
}

// Save the user to the database
func saveUserToDatabase(username, hashedPassword string) error {
    // Use a secure connection string (replace with actual credentials)
    dsn := "user:password@tcp(localhost:3306)/securedb"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        return err
    }
    defer db.Close()

    // Use prepared statements to prevent SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
    if err != nil {
        return err
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword)
    return err
}
