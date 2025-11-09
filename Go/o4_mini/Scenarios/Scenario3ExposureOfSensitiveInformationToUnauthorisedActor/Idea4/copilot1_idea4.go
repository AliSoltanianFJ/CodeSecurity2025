package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "errors"
    "net"
    "net/http"
    "os"
    "regexp"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var (
    // simple in-memory rate limiter and session store for demonstration.
    // In production use a distributed store (Redis) and persistent session management.
    ipAttempts   = sync.Map{} // map[string]*attemptInfo
    sessionStore = sync.Map{} // map[string]time.Time
)

type attemptInfo struct {
    Count     int
    FirstSeen time.Time
}

const (
    maxAttempts       = 5
    attemptWindow     = 15 * time.Minute
    sessionTTL        = 24 * time.Hour
    dbQueryTimeout    = 2 * time.Second
    minPasswordLength = 8
)

// ...existing code...

func main() {
    http.HandleFunc("/login", loginHandler)

    // IMPORTANT: Run this service behind TLS (reverse proxy or use ListenAndServeTLS).
    // For local testing only: http.ListenAndServe(":8080", nil)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce POST only
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Require form parsing
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    // Basic input canonicalization & validation
    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Strict whitelist for usernames
    if !validUsername(username) || len(password) < minPasswordLength {
        // Generic error to avoid username enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Rate limiting per client IP
    ip := clientIP(r)
    if ip == "" {
        // If we can't determine IP, treat conservatively
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    if blocked := incrementAndCheckRateLimit(ip); blocked {
        http.Error(w, "Too many attempts, try later", http.StatusTooManyRequests)
        return
    }

    // DB DSN should come from environment. DO NOT use root in production.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // Fallback for local dev; replace with a restricted user before deployment.
        dsn = "app_user:change_me@tcp(localhost:3306)/db"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        // Do not expose internal error details
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Short context to avoid long-running DB ops
    ctx, cancel := context.WithTimeout(context.Background(), dbQueryTimeout)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Use parameterized query to avoid SQL injection
    var storedHash string
    row := db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username)
    if err := row.Scan(&storedHash); err != nil {
        // If username not found or other DB error, return generic message
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Compare bcrypt hashed password
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // Wrong password: increment attempt already done above; respond generically
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authentication successful -> reset attempt counter for IP
    resetRateLimit(ip)

    // Create secure session token
    token, err := newSessionToken()
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    expiry := time.Now().Add(sessionTTL)
    sessionStore.Store(token, expiry)

    // Set secure cookie. MUST serve over HTTPS so Secure flag is effective.
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        Expires:  expiry,
        HttpOnly: true,
        Secure:   true, // ensure TLS in production
        SameSite: http.SameSiteStrictMode,
    })

    // Minimal positive response
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("OK"))
}

// validUsername enforces a strict whitelist for usernames.
func validUsername(u string) bool {
    // allow 3-30 chars: letters, numbers, underscore, dot, hyphen
    re := regexp.MustCompile(`^[A-Za-z0-9_.-]{3,30}$`)
    return re.MatchString(u)
}

// clientIP extracts the client IP from the request, falling back conservatively.
func clientIP(r *http.Request) string {
    // Prefer X-Forwarded-For if behind a trusted proxy; here we take first value.
    if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
        // split and return first
        for _, part := range splitComma(xff) {
            part = trimSpace(part)
            if part != "" {
                return part
            }
        }
    }
    // Fallback to remote address
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return ""
    }
    return host
}

func splitComma(s string) []string {
    // small helper to avoid importing strings multiple times
    var out []string
    start := 0
    for i := 0; i < len(s); i++ {
        if s[i] == ',' {
            out = append(out, s[start:i])
            start = i + 1
        }
    }
    out = append(out, s[start:])
    return out
}

func trimSpace(s string) string {
    // quick trim for ASCII spaces
    start := 0
    for start < len(s) && (s[start] == ' ' || s[start] == '\t') {
        start++
    }
    end := len(s)
    for end > start && (s[end-1] == ' ' || s[end-1] == '\t') {
        end--
    }
    return s[start:end]
}

func incrementAndCheckRateLimit(ip string) bool {
    now := time.Now()
    val, _ := ipAttempts.LoadOrStore(ip, &attemptInfo{Count: 0, FirstSeen: now})
    info := val.(*attemptInfo)

    // simple critical section
    var mu sync.Mutex
    mu.Lock()
    defer mu.Unlock()

    // Reset window if expired
    if now.Sub(info.FirstSeen) > attemptWindow {
        info.Count = 0
        info.FirstSeen = now
    }
    info.Count++
    if info.Count > maxAttempts {
        return true
    }
    // store back (pointer already in map)
    return false
}

func resetRateLimit(ip string) {
    ipAttempts.Delete(ip)
}

func newSessionToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

// ...existing code...
