package main

import (
    "context"
    "crypto/hmac"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "encoding/hex"
    "fmt"
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

    // limit request body to avoid large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // input validation: username and password constraints
    if len(username) < 3 || len(username) > 64 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    // allow a conservative set of characters for username
    validUser := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // use DSN from environment when possible (avoid hardcoding credentials)
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    // create a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Service unavailable", http.StatusServiceUnavailable)
        return
    }
    defer db.Close()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Service unavailable", http.StatusServiceUnavailable)
        return
    }

    // parameterized query to prevent SQL injection
    var passwordHash string
    const query = "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    row := db.QueryRowContext(ctx, query, username)
    if err := row.Scan(&passwordHash); err != nil {
        // don't reveal whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // compare password using bcrypt (constant-time)
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // create a simple HMAC-based stateless session token (server should verify this token on protected endpoints)
    // SESSION_SECRET should be set in the environment in production
    secret := os.Getenv("SESSION_SECRET")
    if secret == "" {
        secret = "dev-only-secret" // fallback for local testing only
    }
    tokenData := fmt.Sprintf("%s|%d", username, time.Now().Unix())
    h := hmac.New(sha256.New, []byte(secret))
    h.Write([]byte(tokenData))
    sig := hex.EncodeToString(h.Sum(nil))
    raw := tokenData + "|" + sig
    token := base64.RawURLEncoding.EncodeToString([]byte(raw))

    // set secure cookie attributes: HttpOnly, SameSite, Secure when TLS is used
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
        Secure:   r.TLS != nil,
    }
    http.SetCookie(w, cookie)

    // generic success response (do not echo sensitive data)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintln(w, "Login successful")
}
// ...existing code...
