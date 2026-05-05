package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "fmt"
    "net"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    // simple in-memory rate limiter
    attempts   = make(map[string]*attemptInfo)
    attemptsMu sync.Mutex

    usernameWhitelist = regexp.MustCompile(`^[A-Za-z0-9._-]{3,32}$`)
    maxAttempts       = 5
    windowDuration    = 5 * time.Minute
)

type attemptInfo struct {
    count      int
    firstStamp time.Time
}

type credRequest struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

type credResponse struct {
    Moderator bool `json:"moderator"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)

    // NOTE: Serve TLS in production. Here we listen on :8080 for local testing.
    // It's strongly recommended to run behind a TLS-terminating proxy or use ListenAndServeTLS with valid certs.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Println("server failed:", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (minimal, add more via reverse proxy)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Only accept POST with JSON body to avoid leaking creds in URLs/logs
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if ct := r.Header.Get("Content-Type"); !strings.HasPrefix(ct, "application/json") {
        http.Error(w, "unsupported media type", http.StatusUnsupportedMediaType)
        return
    }

    ip := clientIP(r)
    if blocked := rateLimitExceeded(ip); blocked {
        http.Error(w, "too many requests", http.StatusTooManyRequests)
        return
    }

    // parse JSON body
    var req credRequest
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&req); err != nil {
        registerAttempt(ip)
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    // input validation (strict whitelist)
    req.Username = strings.TrimSpace(req.Username)
    if !usernameWhitelist.MatchString(req.Username) || len(req.Password) < 8 || len(req.Password) > 1024 {
        registerAttempt(ip)
        http.Error(w, "unauthorized", http.StatusUnauthorized)
        return
    }

    ok, err := isModerator(req.Username, req.Password)
    if err != nil {
        // internal server errors should not reveal details
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if ok {
        // reset attempts on success
        resetAttempts(ip)
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(credResponse{Moderator: ok})
}

// isModerator checks credentials against environment-backed values.
// Expected environment variables:
//   MOD_USERNAME - the allowed moderator username (exact match)
//   MOD_PASSWORD_HASH - bcrypt hash of the corresponding password
func isModerator(username, password string) (bool, error) {
    envUser := os.Getenv("MOD_USERNAME")
    envHash := os.Getenv("MOD_PASSWORD_HASH")
    if envUser == "" || envHash == "" {
        return false, errors.New("missing server configuration")
    }

    // Constant-time compare for username to reduce username enumeration via timing
    userEqual := subtle.ConstantTimeCompare([]byte(username), []byte(envUser)) == 1
    if !userEqual {
        // do a fake bcrypt compare to make timing more uniform
        _ = bcrypt.CompareHashAndPassword([]byte(envHash), []byte("invalid-password-for-timing"))
        return false, nil
    }

    // Compare password using bcrypt (constant-time for hash compare)
    if err := bcrypt.CompareHashAndPassword([]byte(envHash), []byte(password)); err != nil {
        return false, nil
    }
    return true, nil
}

func clientIP(r *http.Request) string {
    // Prefer X-Forwarded-For if present (take first IP). Only trust this if your proxy sets it.
    xff := r.Header.Get("X-Forwarded-For")
    if xff != "" {
        parts := strings.Split(xff, ",")
        ip := strings.TrimSpace(parts[0])
        return ip
    }
    // Fallback to remote address
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return host
}

func rateLimitExceeded(ip string) bool {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()
    info, ok := attempts[ip]
    if !ok {
        return false
    }
    now := time.Now()
    if now.Sub(info.firstStamp) > windowDuration {
        // window expired
        delete(attempts, ip)
        return false
    }
    return info.count >= maxAttempts
}

func registerAttempt(ip string) {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()
    now := time.Now()
    info, ok := attempts[ip]
    if !ok {
        attempts[ip] = &attemptInfo{count: 1, firstStamp: now}
        return
    }
    if now.Sub(info.firstStamp) > windowDuration {
        *info = attemptInfo{count: 1, firstStamp: now}
        return
    }
    info.count++
}

func resetAttempts(ip string) {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()
    delete(attempts, ip)
}
