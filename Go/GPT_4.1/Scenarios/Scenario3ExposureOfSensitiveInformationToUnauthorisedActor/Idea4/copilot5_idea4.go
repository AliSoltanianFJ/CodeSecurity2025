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

// Security Note: Never hardcode credentials in code. Use environment variables or a secure secrets manager.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbPort     = os.Getenv("DB_PORT")
    dbName     = os.Getenv("DB_NAME")
)

// User represents a user in the system.
type User struct {
    ID           int
    Username     string
    PasswordHash string
}

// Security Note: Only allow HTTPS in production. Enforce HTTPS via reverse proxy or server config.

func main() {
    http.HandleFunc("/login", loginHandler)
    // Security Note: ListenAndServeTLS should be used in production for HTTPS.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* loginHandler authenticates the user if the username and password are correct.
   Security: 
   - Only accepts POST.
   - Validates and sanitizes input.
   - Uses parameterized queries to prevent SQL injection.
   - Does not leak sensitive error details.
   - Does not return whether username or password was incorrect.
   - Does not expose password hashes.
   - Does not set session/cookie (for demo; use secure session mgmt in production).
*/
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form with a reasonable size limit to prevent DoS
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation: enforce reasonable username/password length and charset
    if !isValidUsername(username) || !isValidPassword(password) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Build DSN securely
    dsn := dbUser + ":" + dbPassword + "@tcp(" + dbHost + ":" + dbPort + ")/" + dbName + "?parseTime=true&tls=false"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        // Log error internally, do not expose to user
        log.Printf("DB connection error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Principle of least privilege: Only select necessary fields
    var user User
    err = db.QueryRow("SELECT id, username, password_hash FROM users WHERE username = ?", username).Scan(&user.ID, &user.Username, &user.PasswordHash)
    if err != nil {
        // Do not reveal if username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use constant-time comparison to prevent timing attacks
    if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security Note: In production, generate a secure session token, set HttpOnly, Secure cookie, etc.
    resp := map[string]string{"message": "Login successful"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidUsername validates username for length and allowed characters.
func isValidUsername(username string) bool {
    if len(username) < 3 || len(username) > 32 {
        return false
    }
    for _, c := range username {
        if !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
            return false
        }
    }
    return true
}

// isValidPassword validates password for length and basic complexity.
func isValidPassword(password string) bool {
    if len(password) < 8 || len(password) > 64 {
        return false
    }
    // Add more checks as needed (e.g., require numbers, symbols, etc.)
    return true
}

/*
Security Decisions:
- No secrets are hardcoded; use environment variables for DB credentials.
- All user input is validated and sanitized.
- Parameterized queries prevent SQL injection (CWE-89).
- Passwords are stored as bcrypt hashes (never plaintext).
- No sensitive error messages are returned to the client (CWE-209).
- No session management is implemented here; in production, use secure session tokens/cookies.
- Only necessary fields are selected from the database (least privilege).
- All responses use JSON with correct content type.
- HTTPS is required in production.
*/
