package main

import (
    "database/sql"
    "errors"
    "html"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)
    passwordRegex = regexp.MustCompile(`^[\S]{8,64}$`)
)

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
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
    if !usernameRegex.MatchString(username) || !passwordRegex.MatchString(password) {
        http.Error(w, "Invalid username or password format", http.StatusBadRequest)
        return
    }

    // Escape output to prevent XSS
    safeUsername := html.EscapeString(username)

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Set a reasonable connection timeout
    db.SetConnMaxLifetime(2 * time.Minute)
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)

    // Use prepared statements to prevent SQL injection
    var hashedPassword string
    stmt, err := db.Prepare("SELECT password FROM users WHERE username = ? LIMIT 1")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    err = stmt.QueryRow(username).Scan(&hashedPassword)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password using bcrypt
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Set a secure session cookie (example, not a full session implementation)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    generateSessionToken(), // Implement a secure random token generator
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        MaxAge:   3600,
    })

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte("Welcome, " + safeUsername + "!"))
}

// generateSessionToken generates a secure random session token.
// In production, use crypto/rand and encoding/base64.
func generateSessionToken() string {
    return "secureRandomToken123" // Placeholder: replace with secure random generation
}
