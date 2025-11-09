// ...existing code...
package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "fmt"
    "io/ioutil"
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
    // moderators maps username -> bcrypt hash
    moderators   map[string]string
    modMutex     sync.RWMutex
    usernameRe   = regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    dummyBcrypt  = []byte("$2a$12$u1mYhK9Ymz1Q5q6G2i9uRe9p6YxgY8wz1OqKf1Vw9Qx6c8b1A0p6.") // random bcrypt-like dummy
    rateLimiter  = NewSimpleRateLimiter(5, 15*time.Minute)                                                        // 5 failures -> 15m block
    modsLoadOnce sync.Once
)

// Simple in-memory rate limiter suitable for single-instance demos.
// For production, use a distributed store (Redis) to enforce global limits.
type SimpleRateLimiter struct {
    mu           sync.Mutex
    failures     map[string]*failureInfo
    threshold    int
    blockOnExceed time.Duration
}

type failureInfo struct {
    count      int
    firstSeen  time.Time
    blockUntil time.Time
}

func NewSimpleRateLimiter(threshold int, blockOnExceed time.Duration) *SimpleRateLimiter {
    return &SimpleRateLimiter{
        failures:     make(map[string]*failureInfo),
        threshold:    threshold,
        blockOnExceed: blockOnExceed,
    }
}

func (rl *SimpleRateLimiter) IsBlocked(key string) bool {
    rl.mu.Lock()
    defer rl.mu.Unlock()
    fi, ok := rl.failures[key]
    if !ok {
        return false
    }
    if time.Now().Before(fi.blockUntil) {
        return true
    }
    // expired block: reset record
    if fi.blockUntil.IsZero() && time.Since(fi.firstSeen) > rl.blockOnExceed {
        delete(rl.failures, key)
        return false
    }
    // if block expired, reset
    if time.Now().After(fi.blockUntil) {
        delete(rl.failures, key)
        return false
    }
    return false
}

func (rl *SimpleRateLimiter) RegisterFailure(key string) {
    rl.mu.Lock()
    defer rl.mu.Unlock()
    fi, ok := rl.failures[key]
    if !ok {
        fi = &failureInfo{count: 1, firstSeen: time.Now()}
        rl.failures[key] = fi
        return
    }
    fi.count++
    if fi.count >= rl.threshold {
        fi.blockUntil = time.Now().Add(rl.blockOnExceed)
    }
}

func (rl *SimpleRateLimiter) Reset(key string) {
    rl.mu.Lock()
    defer rl.mu.Unlock()
    delete(rl.failures, key)
}

// loadModerators initializes the moderators map from MODS_FILE or MODS_JSON env var.
// The file or JSON must be a map[string]string with bcrypt hashes as values.
func loadModerators() error {
    modsLoadOnce.Do(func() {
        moderators = map[string]string{}
        var data []byte
        if path := os.Getenv("MODS_FILE"); path != "" {
            b, err := ioutil.ReadFile(path)
            if err != nil {
                // leave moderators empty and return error later
                data = nil
                _ = err
            } else {
                data = b
            }
        } else if j := os.Getenv("MODS_JSON"); j != "" {
            data = []byte(j)
        }

        if len(data) == 0 {
            // No credentials source configured. Keep map empty (no moderators).
            return
        }

        var parsed map[string]string
        if err := json.Unmarshal(data, &parsed); err != nil {
            // invalid format — ignore load to avoid using unsafe data
            return
        }

        for u, h := range parsed {
            u = strings.TrimSpace(u)
            if !usernameRe.MatchString(u) {
                // skip invalid username keys
                continue
            }
            // Basic bcrypt hash sanity check
            if !(strings.HasPrefix(h, "$2a$") || strings.HasPrefix(h, "$2b$") || strings.HasPrefix(h, "$2y$")) {
                continue
            }
            moderators[u] = h
        }
    })
    if moderators == nil {
        moderators = map[string]string{}
    }
    return nil
}

func main() {
    // Load moderators at startup; in production fail fast if secrets missing.
    if err := loadModerators(); err != nil {
        fmt.Fprintln(os.Stderr, "failed to load moderators:", err)
    }

    http.HandleFunc("/check_mod", checkModHandler)

    // NOTE: For production deploy behind TLS/HTTPS (e.g., reverse proxy with TLS) and set HSTS.
    fmt.Println("Listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Fprintln(os.Stderr, "server error:", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")

    // Only allow GET (read-only check). Reject other methods.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse inputs
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    // Validate inputs strictly
    if !usernameRe.MatchString(username) || len(password) == 0 || len(password) > 256 {
        // Generic response to avoid revealing why it failed
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    // Determine client IP for rate limiting (X-Forwarded-For fallback)
    clientIP := clientIPFromRequest(r)

    // Simple rate limiting / lockout
    if rateLimiter.IsBlocked(clientIP) {
        http.Error(w, "too many attempts", http.StatusTooManyRequests)
        return
    }

    // Ensure moderators loaded
    modMutex.RLock()
    if moderators == nil {
        modMutex.RUnlock()
        // Attempt to reload in case it wasn't loaded at startup
        loadModerators()
        modMutex.RLock()
    }
    // copy map pointer safely
    localMods := moderators
    modMutex.RUnlock()

    // Lookup bcrypt hash. If missing, use dummy hash to mitigate username enumeration timing.
    hash, ok := localMods[username]
    if !ok {
        hash = string(dummyBcrypt)
    }

    // Compare password using bcrypt (constant-time for hashes)
    err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
    isMod := (err == nil) && ok // only true if comparison succeeded and username actually exists

    // Update rate limiter on failure
    if !isMod {
        rateLimiter.RegisterFailure(clientIP)
        w.WriteHeader(http.StatusOK)
        _, _ = w.Write([]byte("false"))
        return
    }

    // Success: reset any failure state for this IP
    rateLimiter.Reset(clientIP)

    // Response should be minimal (no PII)
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("true"))
}

// clientIPFromRequest extracts the client IP with X-Forwarded-For fallback.
// It returns a single IP (no port). If parsing fails, returns remote address as-is.
func clientIPFromRequest(r *http.Request) string {
    // Prefer X-Forwarded-For if present (when behind a proxy)
    if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
        parts := strings.Split(xff, ",")
        // take first non-empty trimmed part
        for _, p := range parts {
            ip := strings.TrimSpace(p)
            if ip != "" {
                return ip
            }
        }
    }
    // Fall back to RemoteAddr
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        // could not split, return raw remote addr
        return r.RemoteAddr
    }
    return host
}

// Utility (not used in current code) for safe string comparison.
func constantTimeEquals(a, b string) bool {
    if len(a) != len(b) {
        // still perform a comparison to avoid timing leakage
        _ = subtle.ConstantTimeCompare([]byte(a), []byte(a))
        return false
    }
    return subtle.ConstantTimeCompare([]byte(a), []byte(b)) == 1
}

// ...existing code...
