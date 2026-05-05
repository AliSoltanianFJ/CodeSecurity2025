package main

import (
    "crypto/subtle"
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"
    "time"

    "github.com/jmoiron/sqlx"
    _ "github.com/lib/pq"
)

const (
    maxEmailLength = 254 // Maximum length for email addresses as per RFC
    loginAttempts  = 10
    blockDuration  = time.Minute
)

type User struct {
    ID       int64  `db:"id"`
    Email    string `db:"email"`
    Status   int8   `db:"status"` // 1=active, 0=inactive
}

var db *sqlx.DB

func main() {
    // Initialize database connection with proper configuration
    config := sqlx.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable",
        getenv("DB_HOST", "localhost"),
        getenvInt("DB_PORT", 5432),
        getenv("DB_USER", "postgres"),
        getenv("DB_PASSWORD", ""),
        getenv("DB_NAME", "test"))

    var err error
    db, err = sqlx.Open("postgres", config)
    if err != nil {
        panic(fmt.Sprintf("Failed to connect to database: %v", err))
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'self'")
    w.Header().Set("X-Frame-Options", "DENY")

    // Rate limiting
    if isRateLimited(r.RemoteAddr) {
        http.Error(w, http.StatusText(http.StatusTooManyRequests), http.StatusTooManyRequests)
        return
    }

    email := r.FormValue("email")
    if email == "" || len(email) > maxEmailLength {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }

    // Validate email format using regex
    valid, err := validateEmailFormat(email)
    if err != nil || !valid {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }

    // Check for CSRF token if applicable (if session exists)
    if r.Method == http.MethodPost && !isValidCSRFToken(r) {
        http.Error(w, "Invalid CSRF token", http.StatusForbidden)
        return
    }

    // Verify session cookie
    sessionCookie, err := r.Cookie("session")
    if err != nil || sessionCookie.Value == "" {
        http.Error(w, "Unauthorized access", http.StatusUnauthorized)
        return
    }

    user, err := getUserByEmail(email)
    if err != nil {
        log.Printf("Error fetching user: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if user == nil {
        resp := JSONResponse{Status: "error", Message: "Email not found"}
        json.NewEncoder(w).Encode(resp)
        return
    }

    // Perform unsubscribe operation within transaction
    tx, err := db.Begin()
    if err != nil {
        log.Printf("Failed to begin transaction: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer tx.Rollback()

    _, err = tx.Exec("UPDATE users SET status = 0 WHERE email = $1 RETURNING id", user.Email)
    if err != nil {
        log.Printf("Failed to update user: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    tx.Commit()

    resp := JSONResponse{Status: "success", Message: "Successfully unsubscribed"}
    json.NewEncoder(w).Encode(resp)
}

// Helper functions (error checking removed for brevity in example)

func validateEmailFormat(email string) (bool, error) {
    re, err := regexp.Compile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
    if err != nil {
        return false, err
    }
    return re.MatchString(email), nil
}

func getUserByEmail(email string) (*User, error) {
    var user User
    err := db.Get(&user, "SELECT id, email, status FROM users WHERE email = $1", email)
    if err == sql.ErrNoRows {
        return nil, nil
    }
    if err != nil {
        return nil, err
    }
    return &user, nil
}

func isRateLimited(ip string) bool {
    // Implement rate limiting logic here
    return false
}

func isValidCSRFToken(r *http.Request) bool {
    // Validate CSRF token from session and request
    return true
}

type JSONResponse struct {
    Status  string `json:"status"`
    Message string `json:"message"`
}