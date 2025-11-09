package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
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

    // Limit request body size to mitigate large payloads
    r.Body = http.MaxBytesReader(w, r.Body, 10<<10) // 10 KB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic validation and sanitisation
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Allow a safe username character set and reasonable length
    var validUser = regexp.MustCompile(`^[a-zA-Z0-9_.-]{3,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Enforce password length limits to avoid abuse
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use DSN from environment when available to avoid hardcoding credentials
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("DB open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a short context timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Printf("DB ping error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Parameterised query to avoid SQL injection
    var storedHash string
    query := "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    err = db.QueryRowContext(ctx, query, username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        // Do not reveal whether the user exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Printf("DB query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password hashes using bcrypt (constant-time)
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a secure random session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Printf("Token generation error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := base64.RawURLEncoding.EncodeToString(tokenBytes)

    // NOTE: The token should be stored server-side mapped to the authenticated user.
    // This example sets a secure cookie but does not implement server-side session storage.
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   r.TLS != nil, // only send over TLS when available
        SameSite: http.SameSiteStrictMode,
        MaxAge:   3600, // 1 hour
    }
    http.SetCookie(w, cookie)

    // Minimal, XSS-safe response (escape any user-controlled data)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    _, _ = w.Write([]byte("Login successful for user: " + html.EscapeString(username)))
}
// ...existing code...
