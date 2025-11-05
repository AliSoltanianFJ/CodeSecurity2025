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

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1024)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation and sanitization
    if !usernameRegex.MatchString(username) || !passwordRegex.MatchString(password) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use DSN from environment or config in production, not hardcoded
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use context with timeout to avoid hanging connections
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var hashedPassword string
    err = db.QueryRowContext(ctx, "SELECT password FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        // Do not reveal if user exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Compare hashed password
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Set secure session cookie (placeholder, use secure session management in production)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    generateSessionToken(), // Implement a secure random token generator
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
    })

    // Respond with a generic success message
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte("Login successful"))
}

// generateSessionToken generates a secure random session token.
// In production, use crypto/rand and base64 encoding.
func generateSessionToken() string {
    return "secureRandomToken" // Placeholder: replace with secure implementation
}
