// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "errors"
    "net"
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
    db            *sql.DB
    initOnce      sync.Once
    sessions      sync.Map // map[string]session
    loginAttempts sync.Map // map[string]*attemptTracker
)

type session struct {
    Username string
    Expiry   time.Time
}

type attemptTracker struct {
    Count     int
    FirstSeen time.Time
    mu        sync.Mutex
}

const (
    maxAttempts       = 5
    attemptWindow     = 15 * time.Minute
    sessionExpiry     = 24 * time.Hour
    dbQueryTimeout    = 2 * time.Second
    minPasswordLength = 8
    maxUsernameLen    = 64
)

func main() {
    initDB()
    http.HandleFunc("/login", loginHandler)
    // In production run behind TLS-terminating proxy or serve TLS directly.
    http.ListenAndServe(":8080", nil)
}

func initDB() {
    initOnce.Do(func() {
        // Prefer environment variable for DSN. Example: APP_DSN="appuser:strongpass@tcp(db:3306)/appdb"
        dsn := strings.TrimSpace(os.Getenv("APP_DSN"))
        if dsn == "" {
            // Fallback only for local testing. Do NOT use root in production; create a least-privileged DB user.
            dsn = "root:root@tcp(localhost:3306)/db"
        }
        var err error
        db, err = sql.Open("mysql", dsn)
        if err != nil {
            // Do not leak DSN or internal details to stdout in production.
            panic("database initialization failed")
        }
        // Reasonable pool limits; tune for your environment.
        db.SetConnMaxLifetime(5 * time.Minute)
        db.SetMaxOpenConns(10)
        db.SetMaxIdleConns(5)
    })
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce POST only for state-changing auth operations
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Require TLS to prevent credential interception
    if r.TLS == nil && !isLocalhost(r.RemoteAddr) {
        // Allow non-TLS on localhost for local dev; otherwise require HTTPS
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // Basic input normalization and validation
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if err := validateCredentialsInput(username, password); err != nil {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Rate limiting per IP to mitigate brute-force
    ip := clientIP(r)
    if blocked := checkRateLimit(ip); blocked {
        http.Error(w, "Too many attempts, please try later", http.StatusTooManyRequests)
        return
    }

    // Query DB for password hash using context timeout and prepared statement
    ctx, cancel := context.WithTimeout(r.Context(), dbQueryTimeout)
    defer cancel()

    var storedHash string
    query := "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    err := db.QueryRowContext(ctx, query, username).Scan(&storedHash)
    if err != nil {
        // Do not reveal whether the username exists. Increment attempt tracker.
        incrementAttempt(ip)
        // Log minimal server-side info for monitoring (avoid logging PII)
        // log.Printf("authentication failure for ip=%s: db query error", ip)
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Compare password using constant-time bcrypt verification
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        incrementAttempt(ip)
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Successful authentication: reset attempt tracker
    resetAttempt(ip)

    // Create a new session token and store server-side
    token, err := generateSecureToken(32)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    expiry := time.Now().Add(sessionExpiry)
    sessions.Store(token, session{Username: username, Expiry: expiry})

    // Set secure cookie. Secure flag requires HTTPS; in dev it may be ignored by some clients.
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        Expires:  expiry,
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
    }
    http.SetCookie(w, cookie)

    // Don't include PII in response. Return generic success.
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("OK"))
}

// validateCredentialsInput applies strict whitelists and size limits.
func validateCredentialsInput(username, password string) error {
    if len(username) == 0 || len(username) > maxUsernameLen {
        return errors.New("invalid username length")
    }
    // allow letters, digits, dot, underscore, hyphen; adjust per app policy
    validUser := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validUser.MatchString(username) {
        return errors.New("invalid username characters")
    }
    if len(password) < minPasswordLength || len(password) > 256 {
        return errors.New("invalid password length")
    }
    return nil
}

// checkRateLimit returns true if IP exceeded attempt threshold
func checkRateLimit(ip string) bool {
    now := time.Now()
    val, _ := loginAttempts.LoadOrStore(ip, &attemptTracker{Count: 0, FirstSeen: now})
    tracker := val.(*attemptTracker)
    tracker.mu.Lock()
    defer tracker.mu.Unlock()
    // reset window if expired
    if now.Sub(tracker.FirstSeen) > attemptWindow {
        tracker.Count = 0
        tracker.FirstSeen = now
    }
    return tracker.Count >= maxAttempts
}

func incrementAttempt(ip string) {
    now := time.Now()
    val, _ := loginAttempts.LoadOrStore(ip, &attemptTracker{Count: 0, FirstSeen: now})
    tracker := val.(*attemptTracker)
    tracker.mu.Lock()
    defer tracker.mu.Unlock()
    if now.Sub(tracker.FirstSeen) > attemptWindow {
        tracker.Count = 1
        tracker.FirstSeen = now
    } else {
        tracker.Count++
    }
}

func resetAttempt(ip string) {
    loginAttempts.Delete(ip)
}

// generateSecureToken returns a URL-safe base64 token of n bytes
func generateSecureToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// clientIP attempts to extract a reliable client IP (best-effort).
// In production, trust headers only from your trusted proxy.
func clientIP(r *http.Request) string {
    // Check X-Forwarded-For if behind a proxy (only use trusted proxies in prod!)
    if xff := strings.TrimSpace(r.Header.Get("X-Forwarded-For")); xff != "" {
        parts := strings.Split(xff, ",")
        return strings.TrimSpace(parts[0])
    }
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return host
}

// isLocalhost simple check to allow non-TLS for local testing
func isLocalhost(addr string) bool {
    host, _, err := net.SplitHostPort(addr)
    if err != nil {
        host = addr
    }
    return host == "127.0.0.1" || host == "::1" || strings.HasPrefix(host, "localhost")
}

// ...existing code...
