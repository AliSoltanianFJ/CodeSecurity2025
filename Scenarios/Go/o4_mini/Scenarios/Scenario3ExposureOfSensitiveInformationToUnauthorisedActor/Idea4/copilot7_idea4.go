// ...existing code...
package main

import (
    "context"
    "crypto/hmac"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "errors"
    "fmt"
    "io"
    "log"
    "net/http"
    "os"
    "regexp"
    "strconv"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/login", loginHandler)

    // NOTE: In production, serve behind TLS (ListenAndServeTLS or a reverse proxy that enforces TLS).
    // Also add rate limiting, request logging (without PII), and monitoring.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Only POST allowed
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body to mitigate large requests
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    defer r.Body.Close()

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        // Do not leak parse errors (could reveal parser internals)
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation: strict whitelist for username
    if !validUsername(username) || password == "" {
        // Generic unauthorized response to avoid username enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // DB DSN should come from environment; do not hardcode credentials in code.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // Fallback for local dev, but in production require DB_DSN to be set
        dsn = "root:root@tcp(localhost:3306)/db?parseTime=true"
    }

    // Ensure session secret is set
    sessionSecret := os.Getenv("SESSION_SECRET")
    if len(sessionSecret) < 32 {
        // Do not expose secret details
        http.Error(w, "Server misconfiguration", http.StatusInternalServerError)
        return
    }

    // Connect to DB with context timeout to avoid hanging requests
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Use parameterized query (prepared) to avoid SQL injection
    var userID int64
    var passwordHash string
    query := "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1"
    if err := db.QueryRowContext(ctx, query, username).Scan(&userID, &passwordHash); err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // Do not reveal whether username exists
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Compare password using bcrypt (constant-time compare inside library)
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        // Wrong password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Create a signed session token (HMAC of username + expiry). Stateless token avoids DB writes here.
    expiry := time.Now().Add(24 * time.Hour).Unix()
    tokenValue, err := makeSignedSessionToken(sessionSecret, username, expiry)
    if err != nil {
        log.Printf("token generation error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Set cookie with secure flags. Secure flag set only when request is TLS.
    cookie := &http.Cookie{
        Name:     "session",
        Value:    tokenValue,
        Path:     "/",
        Expires:  time.Unix(expiry, 0),
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        // Secure should be true in production behind TLS; set according to request
        Secure: r.TLS != nil,
    }
    http.SetCookie(w, cookie)

    // Minimal response; do not include PII
    w.WriteHeader(http.StatusOK)
    io.WriteString(w, "Login successful")
}

// validUsername enforces a strict whitelist: 3-30 chars, letters, numbers, ., _, -
func validUsername(u string) bool {
    if u == "" {
        return false
    }
    re := regexp.MustCompile(`^[A-Za-z0-9._-]{3,30}$`)
    return re.MatchString(u)
}

// makeSignedSessionToken builds a compact token: base64URL(username:expiry:base64(hmac))
// This is simple stateless HMAC signing. For production consider using well-audited JWT libs or server-side sessions.
func makeSignedSessionToken(secret, username string, expiry int64) (string, error) {
    // Build payload
    payload := username + ":" + strconv.FormatInt(expiry, 10)

    mac := hmac.New(sha256.New, []byte(secret))
    if _, err := mac.Write([]byte(payload)); err != nil {
        return "", err
    }
    sig := mac.Sum(nil)

    // token := base64URL(payload) + "." + base64URL(sig)
    tokenParts := []string{
        base64.RawURLEncoding.EncodeToString([]byte(payload)),
        base64.RawURLEncoding.EncodeToString(sig),
    }
    return strings.Join(tokenParts, "."), nil
}

// ...existing code...
