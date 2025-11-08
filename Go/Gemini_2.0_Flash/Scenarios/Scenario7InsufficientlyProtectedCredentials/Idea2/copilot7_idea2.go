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

    // Sanitize and validate input
    username := html.EscapeString(r.FormValue("username"))
    password := r.FormValue("password")

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

    // Store the user in the database
    err := storeUser(username, hashedPassword)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

// Helper function to validate username
func isValidUsername(username string) bool {
    // Allow only alphanumeric usernames with 3-20 characters
    re := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
    return re.MatchString(username)
}

// Helper function to hash the password
func hashPassword(password string) string {
    hash := sha256.Sum256([]byte(password))
    return hex.EncodeToString(hash[:])
}

// Helper function to store the user in the database
func storeUser(username, hashedPassword string) error {
    // Use a secure database connection (example assumes MySQL)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
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
