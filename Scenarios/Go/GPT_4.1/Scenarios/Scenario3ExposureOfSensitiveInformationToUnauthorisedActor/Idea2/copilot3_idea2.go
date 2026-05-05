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

    // Input validation
    if !usernameRegex.MatchString(username) || !passwordRegex.MatchString(password) {
        http.Error(w, "Invalid username or password format", http.StatusBadRequest)
        return
    }

    // Sanitize input for logging or output (never echo password)
    safeUsername := html.EscapeString(username)

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use context with timeout to avoid hanging connections
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var hashedPassword string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Set secure session cookie (example, use a real session manager in production)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    generateSecureToken(),
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        MaxAge:   3600,
    })

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte("Login successful. Welcome, " + safeUsername + "!"))
}

// generateSecureToken generates a cryptographically secure random token.
func generateSecureToken() string {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return ""
    }
    return fmt.Sprintf("%x", b)
}

// Required imports
import (
    "context"
    "crypto/rand"
    "fmt"
)
