package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "net/http"
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

    // Parse and validate input safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic input validation & limits to avoid abuse
    if len(username) < 3 || len(username) > 64 || len(password) < 8 || len(password) > 256 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    // Restrict allowed username characters to a safe subset
    var validUser = regexp.MustCompile(`^[A-Za-z0-9@._-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Open DB connection with timeout context
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use parameterized query to prevent SQL injection.
    // Expect stored password hashes (bcrypt) in password_hash column.
    var passwordHash string
    var userID int64
    err = db.QueryRowContext(ctx, "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&userID, &passwordHash)
    if err != nil {
        // Do not reveal whether username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Compare password using bcrypt constant-time comparison
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a cryptographically secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := hex.EncodeToString(tokenBytes)
    expires := time.Now().Add(24 * time.Hour)

    // Store session server-side to bind token to user (sessions table expected).
    // Use parameterized statement to avoid injection.
    _, err = db.ExecContext(ctx, "INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE token=VALUES(token), expires_at=VALUES(expires_at)", userID, token, expires)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Set secure cookie flags. Secure requires HTTPS in production.
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        Expires:  expires,
    })

    // Generic success message; do not echo sensitive data back to client.
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
