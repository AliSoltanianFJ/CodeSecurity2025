package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

// Security Note: Never hardcode secrets or credentials in code.
// Use environment variables or a secure secrets manager.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbName     = os.Getenv("DB_NAME")
    dbHost     = os.Getenv("DB_HOST")
    dbPort     = os.Getenv("DB_PORT")
)

// Security Note: Use HTTPS in production to protect data in transit.

func main() {
    http.HandleFunc("/login", loginHandler)
    // Security Note: ListenAndServeTLS should be used in production.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler authenticates a user securely.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security Note: Limit request body size to prevent DoS attacks.
    r.Body = http.MaxBytesReader(w, r.Body, 1048576) // 1MB max

    // Parse form data securely.
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation: enforce username and password policy.
    if !isValidUsername(username) || !isValidPassword(password) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security Note: Use least privilege DB user, avoid root in production.
    dsn := dbUser + ":" + dbPassword + "@tcp(" + dbHost + ":" + dbPort + ")/" + dbName + "?parseTime=true&tls=false"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        // Log error internally, do not leak details to client.
        log.Printf("DB connection error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security Note: Use prepared statements to prevent SQL injection.
    var hashedPassword string
    var userID int
    stmt := `SELECT id, password_hash FROM users WHERE username = ? LIMIT 1`
    err = db.QueryRow(stmt, username).Scan(&userID, &hashedPassword)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // Do not reveal if username exists.
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("DB query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security Note: Use constant-time comparison to prevent timing attacks.
    if err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security Note: Generate a secure session token (not implemented here).
    // Set secure, HttpOnly, SameSite cookies for session management.

    // Respond with minimal information.
    resp := struct {
        Message string `json:"message"`
    }{
        Message: "Login successful",
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidUsername validates the username according to policy.
func isValidUsername(username string) bool {
    // Example: 3-32 chars, alphanumeric and underscores only.
    if len(username) < 3 || len(username) > 32 {
        return false
    }
    for _, c := range username {
        if !(c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
            return false
        }
    }
    return true
}

// isValidPassword validates the password according to policy.
func isValidPassword(password string) bool {
    // Example: 8-64 chars, no spaces, printable ASCII only.
    if len(password) < 8 || len(password) > 64 {
        return false
    }
    for _, c := range password {
        if c < 33 || c > 126 {
            return false
        }
    }
    return true
}
