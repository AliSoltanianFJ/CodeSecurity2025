package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "errors"
    "log"
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
func main() {
    // ...existing code...
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic content-type enforcement (helps middleware and clients)
    if ct := r.Header.Get("Content-Type"); !strings.HasPrefix(ct, "application/x-www-form-urlencoded") && ct != "" {
        http.Error(w, "Unsupported Media Type", http.StatusUnsupportedMediaType)
        return
    }

    // Parse form (with reasonable memory limits default in net/http)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation: strict whitelist for username; length checks
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_]{3,32}$`)
    if !validUser.MatchString(username) || len(password) < 8 || len(password) > 128 {
        // Do not reveal which field failed
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Rate limiting (simple in-memory per-IP limit). For production use a distributed rate limiter.
    ip := clientIP(r)
    if tooManyAttempts(ip) {
        http.Error(w, "Too many attempts, try later", http.StatusTooManyRequests)
        return
    }

    // Database connection: require DSN via environment (fail closed)
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // Internal server misconfiguration; log for operator but don't expose to client.
        log.Println("DB_DSN not set")
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    // Note: in a long-running server, prefer a single shared db instance created in main()
    // and closed on shutdown. Here we ensure this connection is usable.
    ctxPing, cancelPing := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancelPing()
    if err := db.PingContext(ctxPing); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Query the user's hashed password using a parameterised query and context timeout.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var passwordHash string
    query := "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    err = db.QueryRowContext(ctx, query, username).Scan(&passwordHash)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            registerFailedAttempt(ip)
            // Generic response to avoid username enumeration
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password using bcrypt (constant-time)
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        // Wrong password
        registerFailedAttempt(ip)
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Successful authentication: clear failure record
    resetAttempts(ip)

    // Create a secure session token (cryptographically random), store server-side.
    token, err := newSessionToken(32)
    if err != nil {
        log.Printf("session token error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    expiry := time.Now().Add(24 * time.Hour)

    // Store session server-side. In production, persist in DB/Redis and set short TTL.
    storeSession(token, username, expiry)

    // Set cookie with strong security attributes. Ensure application is served over TLS.
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        Expires:  expiry,
        MaxAge:   int(time.Until(expiry).Seconds()),
        Secure:   true,                         // requires TLS termination
        HttpOnly: true,                         // not accessible to JS
        SameSite: http.SameSiteStrictMode,      // strict CSRF protection for login
    }
    http.SetCookie(w, cookie)

    // Minimal success response; avoid echoing any sensitive data.
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("OK"))
}

// ---------------- Helper utilities and in-memory stores (for demo) ----------------

// NOTE: The following in-memory stores are for demonstration and unit testing.
// Production systems should use hardened persistent stores (DB/Redis), rotate keys,
// enforce TTL eviction, and apply additional monitoring & audit logging.

var (
    // simple rate limiter
    attemptsMu sync.Mutex
    attempts   = map[string]attemptRecord{}

    // simple session store
    sessionsMu sync.Mutex
    sessions   = map[string]sessionRecord{}
)

type attemptRecord struct {
    Count     int
    FirstSeen time.Time
}

type sessionRecord struct {
    Username string
    Expiry   time.Time
}

func clientIP(r *http.Request) string {
    // Trust X-Forwarded-For if present (careful in deployments behind proxies)
    xff := r.Header.Get("X-Forwarded-For")
    if xff != "" {
        parts := strings.Split(xff, ",")
        ip := strings.TrimSpace(parts[0])
        if net.ParseIP(ip) != nil {
            return ip
        }
    }
    // Fallback to remote address
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err == nil && net.ParseIP(host) != nil {
        return host
    }
    return r.RemoteAddr
}

const (
    maxAttempts      = 5
    attemptWindow    = 15 * time.Minute
    lockoutThreshold = 10 // optional extended lock
)

func tooManyAttempts(ip string) bool {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()
    rec, ok := attempts[ip]
    if !ok {
        return false
    }
    if time.Since(rec.FirstSeen) > attemptWindow {
        delete(attempts, ip)
        return false
    }
    return rec.Count >= maxAttempts
}

func registerFailedAttempt(ip string) {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()
    rec, ok := attempts[ip]
    if !ok {
        attempts[ip] = attemptRecord{Count: 1, FirstSeen: time.Now()}
        return
    }
    if time.Since(rec.FirstSeen) > attemptWindow {
        attempts[ip] = attemptRecord{Count: 1, FirstSeen: time.Now()}
        return
    }
    rec.Count++
    attempts[ip] = rec
}

func resetAttempts(ip string) {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()
    delete(attempts, ip)
}

func newSessionToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

func storeSession(token, username string, expiry time.Time) {
    sessionsMu.Lock()
    defer sessionsMu.Unlock()
    sessions[token] = sessionRecord{Username: username, Expiry: expiry}
}
