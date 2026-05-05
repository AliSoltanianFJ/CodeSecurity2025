package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
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

    // Limit request size to avoid large payloads
    r.Body = http.MaxBytesReader(w, r.Body, 10<<10) // 10 KB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation and sanitisation
    if len(username) < 3 || len(username) > 32 || len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // Allow only a restricted set of username characters
    validUser := regexp.MustCompile(`^[A-Za-z0-9_.-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use DSN from environment to avoid hardcoding credentials
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use short context timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    // Retrieve password hash for the given username using a parameterised query
    var passwordHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        // Do not reveal whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password using bcrypt
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Create a secure session token
    raw := make([]byte, 32)
    if _, err := rand.Read(raw); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := base64.RawURLEncoding.EncodeToString(raw)
    expires := time.Now().Add(24 * time.Hour)

    // Store session in DB (sessions table expected). Use parameterised query.
    _, err = db.ExecContext(ctx,
        `INSERT INTO sessions (token, username, expires_at) VALUES (?, ?, ?)
         ON DUPLICATE KEY UPDATE username = VALUES(username), expires_at = VALUES(expires_at)`,
        token, username, expires)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Set secure cookie. Secure flag set only when request is TLS.
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        Expires:  expires,
        HttpOnly: true,
        Secure:   r.TLS != nil,
        SameSite: http.SameSiteLaxMode,
    }
    http.SetCookie(w, cookie)

    // Redirect to a non-reflecting URL (avoid echoing user-supplied data)
    http.Redirect(w, r, "/dashboard", http.StatusSeeOther)
}
// ...existing code...
