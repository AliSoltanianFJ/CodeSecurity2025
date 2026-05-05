package main

import (
    "crypto/subtle"
    "encoding/json"
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

var (
    modUsername     string
    modPasswordHash string // bcrypt hash
    usernameRE      = regexp.MustCompile(`^[A-Za-z0-9_.-]{1,64}$`)
    rateLimiter     = newIPRateLimiter(5, time.Minute) // 5 fails per minute
)

// simple in-memory rate limiter per IP for failed auth attempts
type ipLimiter struct {
    mu      sync.Mutex
    clients map[string][]time.Time
    limit   int
    window  time.Duration
}

func newIPRateLimiter(limit int, window time.Duration) *ipLimiter {
    return &ipLimiter{
        clients: make(map[string][]time.Time),
        limit:   limit,
        window:  window,
    }
}

func (il *ipLimiter) addFailure(ip string) {
    il.mu.Lock()
    defer il.mu.Unlock()
    now := time.Now()
    times := il.clients[ip]
    // keep only recent
    clean := times[:0]
    for _, t := range times {
        if now.Sub(t) <= il.window {
            clean = append(clean, t)
        }
    }
    clean = append(clean, now)
    il.clients[ip] = clean
}

func (il *ipLimiter) tooManyFailures(ip string) bool {
    il.mu.Lock()
    defer il.mu.Unlock()
    now := time.Now()
    times := il.clients[ip]
    count := 0
    for _, t := range times {
        if now.Sub(t) <= il.window {
            count++
        }
    }
    return count >= il.limit
}

func init() {
    modUsername = strings.TrimSpace(os.Getenv("MOD_USERNAME"))
    modPasswordHash = strings.TrimSpace(os.Getenv("MOD_PASSWORD_BCRYPT")) // expect bcrypt hash

    if modUsername == "" || modPasswordHash == "" {
        log.Fatal("MOD_USERNAME and MOD_PASSWORD_BCRYPT environment variables must be set. Do NOT hard-code credentials.")
    }
}

// ...existing code...

func main() {
    http.HandleFunc("/check_mod", checkModHandler)

    cert := strings.TrimSpace(os.Getenv("TLS_CERT_PATH"))
    key := strings.TrimSpace(os.Getenv("TLS_KEY_PATH"))
    addr := ":8443"
    if cert != "" && key != "" {
        log.Println("Starting HTTPS server on", addr)
        if err := http.ListenAndServeTLS(addr, cert, key, nil); err != nil {
            log.Fatal("server failed:", err)
        }
        return
    }

    // Fallback only for local testing - strongly prefer TLS in production
    log.Println("WARNING: TLS cert/key not provided, falling back to HTTP on :8080. Do not use in production.")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal("server failed:", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Require POST so credentials are not in URLs or logs
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Strict content-type expectation (form or JSON)
    ct := r.Header.Get("Content-Type")
    if !strings.HasPrefix(ct, "application/x-www-form-urlencoded") && !strings.HasPrefix(ct, "application/json") {
        http.Error(w, "unsupported content type", http.StatusBadRequest)
        return
    }

    // Parse form values if form encoded
    if strings.HasPrefix(ct, "application/x-www-form-urlencoded") {
        if err := r.ParseForm(); err != nil {
            http.Error(w, "invalid form", http.StatusBadRequest)
            return
        }
    }

    // Extract credentials (prefer form, allow JSON body)
    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" && strings.HasPrefix(ct, "application/json") {
        // decode minimal JSON { "username":"x", "password":"y" }
        var body struct {
            Username string `json:"username"`
            Password string `json:"password"`
        }
        dec := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)) // 1MiB limit
        if err := dec.Decode(&body); err != nil {
            http.Error(w, "invalid json body", http.StatusBadRequest)
            return
        }
        username = body.Username
        password = body.Password
    }

    // Basic validation and canonicalization
    username = strings.TrimSpace(username)
    if !usernameRE.MatchString(username) || len(password) > 256 {
        // Do not reveal which part failed to avoid user enumeration
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    ip := clientIP(r)
    if rateLimiter.tooManyFailures(ip) {
        http.Error(w, "too many attempts", http.StatusTooManyRequests)
        return
    }

    // constant-time username comparison to mitigate timing user enumeration
    if subtle.ConstantTimeCompare([]byte(username), []byte(modUsername)) != 1 {
        rateLimiter.addFailure(ip)
        respondBool(w, false)
        return
    }

    // Use bcrypt compare for password (bcrypt hash loaded from env)
    if err := bcrypt.CompareHashAndPassword([]byte(modPasswordHash), []byte(password)); err != nil {
        // failed auth
        rateLimiter.addFailure(ip)
        respondBool(w, false)
        return
    }

    // Success — do not log sensitive data
    respondBool(w, true)
}

// helper: write JSON boolean response with secure headers
func respondBool(w http.ResponseWriter, v bool) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Security headers (set minimal sensible defaults)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    // HSTS only set if served over TLS in front-end/proxy
    resp := struct {
        IsModerator bool `json:"is_moderator"`
    }{IsModerator: v}
    enc := json.NewEncoder(w)
    _ = enc.Encode(resp)
}

// clientIP extracts the originating IP considering X-Forwarded-For (simple, limited)
func clientIP(r *http.Request) string {
    xff := r.Header.Get("X-Forwarded-For")
    if xff != "" {
        parts := strings.Split(xff, ",")
        ip := strings.TrimSpace(parts[0])
        if net.ParseIP(ip) != nil {
            return ip
        }
    }
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err == nil && net.ParseIP(host) != nil {
        return host
    }
    return "unknown"
}

// ...existing code...
