package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

// Security decision: Do not hardcode credentials. Use environment variables or a secure secrets manager.
var dbDSN = os.Getenv("DB_DSN")

// User represents a user in the system.
type User struct {
    ID       int
    Username string
    Password string // hashed password
}

// Security decision: Only allow POST, enforce content-type, and limit request body size.
func main() {
    http.HandleFunc("/login", loginHandler)
    // Security decision: Always use HTTPS in production. This is for demonstration only.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security decision: Enforce content-type and limit body size to prevent DoS.
    if r.Header.Get("Content-Type") != "application/x-www-form-urlencoded" {
        http.Error(w, "Unsupported content type", http.StatusUnsupportedMediaType)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 1024) // Limit request body size

    // Parse and validate input
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Security decision: Validate input length and characters to prevent injection and DoS.
    if !isValidUsername(username) || !isValidPassword(password) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security decision: Use least privilege DB user, do not hardcode DSN.
    db, err := sql.Open("mysql", dbDSN)
    if err != nil {
        log.Printf("DB connection error: %v", err) // Do not leak details to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security decision: Use parameterized queries to prevent SQL injection.
    var user User
    err = db.QueryRow("SELECT id, username, password FROM users WHERE username = ?", username).Scan(&user.ID, &user.Username, &user.Password)
    if errors.Is(err, sql.ErrNoRows) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("DB query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security decision: Use bcrypt for password hashing and comparison.
    if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security decision: Do not expose sensitive information in responses.
    // In production, generate a secure session or JWT token here.
    resp := map[string]string{"message": "Login successful"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidUsername validates the username for length and allowed characters.
func isValidUsername(username string) bool {
    if len(username) < 3 || len(username) > 32 {
        return false
    }
    for _, c := range username {
        if !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '_' && c != '-' {
            return false
        }
    }
    return true
}

// isValidPassword validates the password for length and basic complexity.
func isValidPassword(password string) bool {
    if len(password) < 12 || len(password) > 64 {
        return false
    }
    // Add more checks as needed (e.g., require numbers, symbols, etc.)
    return true
}
