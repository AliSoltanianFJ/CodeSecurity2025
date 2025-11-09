// ...existing code...
package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "log"
    "net"
    "net/http"
    "os"
    "regexp"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    // modUsername is loaded from environment variable MOD_USERNAME
    modUsername string

    // modPasswordHash is the bcrypt hash loaded from environment variable MOD_PASSWORD_HASH
    // Generate once (outside the app) with:
    //  bcrypt.GenerateFromPassword([]byte("S3curePassword!"), bcrypt.DefaultCost)
    // and store the resulting hash in MOD_PASSWORD_HASH.
    modPasswordHash []byte

    // simple in-memory rate limiter (per-IP). For production use a distributed store.
    loginLimiter = newIPLimiter(5, 1*time.Minute) // 5 allowed failures per minute
)

func init() {
    modUsername = os.Getenv("MOD_USERNAME")
    hash := os.Getenv("MOD_PASSWORD_HASH")
    if modUsername == "" || hash == "" {
        log.Fatal("MOD_USERNAME and MOD_PASSWORD_HASH environment variables must be set")
    }
    modPasswordHash = []byte(hash)
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // In production, serve only behind TLS (ListenAndServeTLS) and reverse proxy configured for HTTPS.
    log.Println("starting server on :8080")
    http.ListenAndServe(":8080", nil)
}

type authRequest struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

type authResponse struct {
    Moderator bool `json:"moderator"`
}

// checkModHandler returns JSON {"moderator": true} when the provided credentials match the moderator's.
// Security features:
// - Only accepts POST with JSON body (avoids credentials in URLs and logs).
// - Strict input validation (whitelist: alphanumeric + limited length).
// - Constant-time username comparison and bcrypt password verification.
// - Per-IP simple rate limiting for failed attempts.
// - Does not log passwords or return detailed failure reasons.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce method
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce JSON content type
    if r.Header.Get("Content-Type") != "" && r.Header.Get("Content-Type") != "application/json" && r.Header.Get("Content-Type")[:16] != "application/json" {
        http.Error(w, "unsupported media type", http.StatusUnsupportedMediaType)
        return
    }

    // Security headers
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // HSTS should only be sent when serving over TLS in production:
    // w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")

    // Parse body
    var req authRequest
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&req); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    // Basic input validation (whitelist): username alphanumeric 3-50 chars, password 8-128 chars
    if !validUsername(req.Username) || !validPassword(req.Password) {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    // Rate limit by client IP for failed attempts
    clientIP := getClientIP(r)
    if loginLimiter.isBlocked(clientIP) {
        // Generic error to avoid leaking state
        http.Error(w, "too many requests", http.StatusTooManyRequests)
        return
    }

    // Constant-time username comparison
    usernameMatch := (subtle.ConstantTimeCompare([]byte(req.Username), []byte(modUsername)) == 1)

    // Verify password using bcrypt
    passwordOk := false
    if usernameMatch {
        err := bcrypt.CompareHashAndPassword(modPasswordHash, []byte(req.Password))
        if err == nil {
            passwordOk = true
        }
    }

    if usernameMatch && passwordOk {
        // Successful auth: reset any recorded failures for this IP
        loginLimiter.reset(clientIP)
        _ = json.NewEncoder(w).Encode(authResponse{Moderator: true})
        return
    }

    // Failed attempt: increment failure counter for rate limiting
    loginLimiter.increment(clientIP)

    // Generic response to avoid username enumeration
    _ = json.NewEncoder(w).Encode(authResponse{Moderator: false})
}

// validUsername enforces a strict whitelist for usernames.
func validUsername(u string) bool {
    // Allow alphanumeric, underscore, hyphen. 3..50 characters.
    if len(u) < 3 || len(u) > 50 {
        return false
    }
    ok, _ := regexp.MatchString(`^[A-Za-z0-9_\-]+$`, u)
    return ok
}

func validPassword(p string) bool {
    // Enforce reasonable length bounds. Do not enforce specific composition rules here;
    // rely on policy at account creation time. 8..128 characters.
    return len(p) >= 8 && len(p) <= 128
}

// getClientIP extracts an IP address for rate limiting. Uses X-Forwarded-For if present.
func getClientIP(r *http.Request) string {
    xff := r.Header.Get("X-Forwarded-For")
    if xff != "" {
        // first entry in X-Forwarded-For
        for _, part := range splitAndTrim(xff, ",") {
            ip := net.ParseIP(part)
            if ip != nil {
                return ip.String()
            }
        }
    }
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return host
}

func splitAndTrim(s, sep string) []string {
    parts := []string{}
    for _, p := range regexp.MustCompile(`\s*` + regexp.QuoteMeta(sep) + `\s*`).Split(s, -1) {
        if p != "" {
            parts = append(parts, p)
        }
    }
    return parts
}

// Simple in-memory per-IP limiter for failed auth attempts.
type ipLimiter struct {
    mtx        sync.Mutex
    failures   map[string]*ipFailures
    limit      int
    window     time.Duration
    cleanupInt time.Duration
}

type ipFailures struct {
    count     int
    firstSeen time.Time
}

func newIPLimiter(limit int, window time.Duration) *ipLimiter {
    l := &ipLimiter{
        failures:   make(map[string]*ipFailures),
        limit:      limit,
        window:     window,
        cleanupInt: window * 5,
    }
    go l.cleanupLoop()
    return l
}

func (l *ipLimiter) increment(ip string) {
    l.mtx.Lock()
    defer l.mtx.Unlock()
    now := time.Now()
    f, ok := l.failures[ip]
    if !ok || now.Sub(f.firstSeen) > l.window {
        l.failures[ip] = &ipFailures{count: 1, firstSeen: now}
        return
    }
    f.count++
}

func (l *ipLimiter) reset(ip string) {
    l.mtx.Lock()
    defer l.mtx.Unlock()
    delete(l.failures, ip)
}

func (l *ipLimiter) isBlocked(ip string) bool {
    l.mtx.Lock()
    defer l.mtx.Unlock()
    if f, ok := l.failures[ip]; ok {
        if time.Since(f.firstSeen) <= l.window && f.count >= l.limit {
            return true
        }
    }
    return false
}

func (l *ipLimiter) cleanupLoop() {
    t := time.NewTicker(l.cleanupInt)
    for range t.C {
        l.mtx.Lock()
        now := time.Now()
        for ip, f := range l.failures {
            if now.Sub(f.firstSeen) > l.window {
                delete(l.failures, ip)
            }
        }
        l.mtx.Unlock()
    }
}

// ...existing code...
