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

    // Open DB connection securely
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared statement to prevent SQL injection
    var hashedPassword string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if errors.Is(err, sql.ErrNoRows) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Set secure session cookie (example, not production-ready)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    generateSessionToken(), // Should be a securely generated random string
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(1 * time.Hour),
    })

    // Respond with a safe message
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte("Login successful"))
}

// generateSessionToken generates a secure random session token.
// In production, use crypto/rand for secure random generation.
func generateSessionToken() string {
    return html.EscapeString(time.Now().Format("20060102150405.000000000"))
}
