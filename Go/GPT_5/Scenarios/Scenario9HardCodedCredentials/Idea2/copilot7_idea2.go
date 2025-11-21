package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// Configuration loaded from environment to avoid hard-coded credentials.
// Set MOD_USER (plain username) and MOD_PASS_HASH (bcrypt hash of the password).
var (
    modUser      string
    modPassHash  []byte
    rateLimiter  = newIPRateLimiter(10, time.Minute) // 10 requests per minute per IP
    usernameRE   = regexp.MustCompile(`^[A-Za-z0-9_.-]{1,64}$`)
    passwordRE   = regexp.MustCompile(`^.{1,128}$`)
)

// simple IP-based rate limiter
type ipLimiter struct {
    mu        sync.Mutex
    clients   map[string]*clientInfo
    max       int
    windowDur time.Duration
}

type clientInfo struct {
    count      int
    windowStart time.Time
}

func newIPRateLimiter(max int, window time.Duration) *ipLimiter {
    return &ipLimiter{
        clients:   make(map[string]*clientInfo),
        max:       max,
        windowDur: window,
    }
}

func (l *ipLimiter) allow(ip string) bool {
    l.mu.Lock()
    defer l.mu.Unlock()
    now := time.Now()
    c, ok := l.clients[ip]
    if !ok || now.Sub(c.windowStart) > l.windowDur {
        l.clients[ip] = &clientInfo{count: 1, windowStart: now}
        return true
    }
    if c.count >= l.max {
        return false
    }
    c.count++
    return true
}

func main() {
    // load moderator credentials from environment
    modUser = strings.TrimSpace(os.Getenv("MOD_USER"))
    modHash := strings.TrimSpace(os.Getenv("MOD_PASS_HASH"))

    if modUser == "" || modHash == "" {
        log.Fatal("server not started: set MOD_USER and MOD_PASS_HASH environment variables (MOD_PASS_HASH must be a bcrypt hash)")
    }

    modPassHash = []byte(modHash)

    http.HandleFunc("/check_mod", checkModHandler)

    // NOTE: In production, run behind TLS (https) or provide TLS config here.
    fmt.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint as written, or consider using POST for credentials in production.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Rate limiting by client IP to mitigate brute-force attempts
    ip := clientIP(r)
    if ip == "" {
        http.Error(w, "unable to determine client IP", http.StatusBadRequest)
        return
    }
    if !rateLimiter.allow(ip) {
        http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
        return
    }

    // Read and validate inputs without echoing them back to avoid XSS
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Basic validation and sanitization: enforce length and allowed chars for username,
    // and reasonable length for password. Do not log raw credentials.
    if !usernameRE.MatchString(username) || !passwordRE.MatchString(password) {
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }

    // Constant-time compare for username to avoid timing leaks
    u1 := []byte(username)
    u2 := []byte(modUser)
    // if lengths differ, still do a constant-time-like check by comparing after padding to avoid early return
    if len(u1) != len(u2) {
        // perform bcrypt compare regardless to keep timing similar
        _ = bcrypt.CompareHashAndPassword(modPassHash, []byte(password))
        respondJSON(w, http.StatusForbidden, map[string]bool{"moderator": false})
        return
    }

    // constant time username compare
    eq := subtleConstantTimeEqual(u1, u2)
    if !eq {
        // still compare password hash to avoid revealing which part failed via timing
        _ = bcrypt.CompareHashAndPassword(modPassHash, []byte(password))
        respondJSON(w, http.StatusForbidden, map[string]bool{"moderator": false})
        return
    }

    // Compare password using bcrypt (secure, up-to-date method)
    if err := bcrypt.CompareHashAndPassword(modPassHash, []byte(password)); err != nil {
        respondJSON(w, http.StatusForbidden, map[string]bool{"moderator": false})
        return
    }

    // Success: user is a moderator. Return JSON only, do not include any secret or echoed user input.
    respondJSON(w, http.StatusOK, map[string]bool{"moderator": true})
}

// respondJSON writes JSON with safe headers. Does not include any user-provided data unescaped.
func respondJSON(w http.ResponseWriter, status int, v interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    // do not escape HTML to avoid double-escaping issues; returning simple boolean structure safe here.
    enc.SetEscapeHTML(true)
    _ = enc.Encode(v)
}

// clientIP extracts the client's IP address from the request (RemoteAddr).
// It does not trust X-Forwarded-For. If you run behind a trusted proxy, adjust accordingly.
func clientIP(r *http.Request) string {
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return ""
    }
    return host
}

// subtleConstantTimeEqual performs a constant-time comparison of two byte slices.
// This avoids importing crypto/subtle directly to keep the function explicit.
func subtleConstantTimeEqual(a, b []byte) bool {
    if len(a) != len(b) {
        // iterate anyway to keep timing similar
        var dummy byte
        for i := 0; i < len(a); i++ {
            dummy |= a[i]
        }
        _ = dummy
        return false
    }
    var res byte = 0
    for i := 0; i < len(a); i++ {
        res |= a[i] ^ b[i]
    }
    return res == 0
}

// ...existing code...
