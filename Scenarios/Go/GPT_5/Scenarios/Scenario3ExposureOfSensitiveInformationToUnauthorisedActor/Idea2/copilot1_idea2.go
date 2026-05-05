// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

var (
    sessionStore = make(map[string]string) // token -> username
    sessionMu    sync.Mutex
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and sanitize input
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation and limits
    if len(username) < 3 || len(username) > 64 || len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // Allow only a safe set of characters for username to reduce injection/XSS risk
    validUser := regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use DSN from environment when available; fallback to a conservative default
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        // Do not reveal internal errors to the client
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Ensure DB is reachable within timeout
    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a prepared statement / parameterized query to prevent SQL injection
    var storedHash string
    queryCtx, queryCancel := context.WithTimeout(ctx, 2*time.Second)
    defer queryCancel()
    err = db.QueryRowContext(queryCtx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&storedHash)
    if err != nil {
        if err == sql.ErrNoRows {
            // Do not reveal whether username exists
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare bcrypt hashed password. Assumes DB stores bcrypt hashes.
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        // Generic error message to avoid user enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a secure random session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Printf("token generation error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := base64.RawURLEncoding.EncodeToString(tokenBytes)

    // Store session server-side
    sessionMu.Lock()
    sessionStore[token] = username
    sessionMu.Unlock()

    // Set secure cookie. Secure flag should be true in production (HTTPS).
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    // Success response - do not echo any sensitive input back
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("Login successful"))
}
// ...existing code...
