// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var (
    sessionStore = make(map[string]string) // token -> username (in-memory; replace with persistent store in prod)
    sessMu       sync.Mutex
    userRegex    = regexp.MustCompile(`^[A-Za-z0-9@._-]{1,128}$`)
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

    // Parse and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation: avoid excessively long input and unsafe chars
    if len(username) == 0 || len(username) > 128 || !userRegex.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(password) == 0 || len(password) > 256 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use DSN from environment to avoid hard-coding secrets in source.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // Do not reveal internal details to client
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // Open DB with context and timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Verify connection
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Parameterized query to avoid SQL injection. Assume passwords stored as bcrypt hashes in column password_hash.
    var passwordHash string
    queryCtx, qCancel := context.WithTimeout(ctx, 3*time.Second)
    defer qCancel()

    err = db.QueryRowContext(queryCtx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&passwordHash)
    if err != nil {
        // Do not reveal whether username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Compare bcrypt hash (safe, constant-time comparison)
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authenticated: generate secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    token := hex.EncodeToString(tokenBytes)

    // Store session in-memory (for demo). In production, use a secure persistent store.
    sessMu.Lock()
    sessionStore[token] = username
    sessMu.Unlock()

    // Set secure cookie. HttpOnly prevents JS access; Secure requires HTTPS (recommended).
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        MaxAge:   86400, // 1 day
    }
    http.SetCookie(w, cookie)

    // Return a minimal, non-echoing success response to avoid XSS / info leakage
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
