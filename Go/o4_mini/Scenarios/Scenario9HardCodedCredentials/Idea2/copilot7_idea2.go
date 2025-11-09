// ...existing code...
package main

import (
    "encoding/json"
    "log"
    "net"
    "net/http"
    "os"
    "regexp"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
    "crypto/subtle"
)

var (
    modUsername    string
    modPasswordHash string
    attemptsMu     sync.Mutex
    attempts       = make(map[string]*ipAttempt)
)

type ipAttempt struct {
    count      int
    firstStamp time.Time
}

func main() {
    // Load moderator credentials from environment to avoid hard-coding secrets in source.
    // Set MOD_USERNAME and MOD_PASSWORD_HASH (bcrypt hash) before running.
    modUsername = os.Getenv("MOD_USERNAME")
    modPasswordHash = os.Getenv("MOD_PASSWORD_HASH")
    if modUsername == "" || modPasswordHash == "" {
        log.Fatal("MOD_USERNAME and MOD_PASSWORD_HASH environment variables must be set")
    }

    http.HandleFunc("/check_mod", checkModHandler)

    // Note: In production, serve over TLS (ListenAndServeTLS) and put behind a reverse proxy.
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET or POST for this endpoint.
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Simple rate limiting per IP to mitigate brute-force attempts.
    ip, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        // If we can't parse the remote address, use the whole string as a key (safe fallback).
        ip = r.RemoteAddr
    }
    if blocked := registerAttempt(ip); blocked {
        http.Error(w, "too many requests", http.StatusTooManyRequests)
        return
    }

    // Read and validate inputs from query parameters.
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Validate presence and reasonable length.
    if len(username) == 0 || len(password) == 0 || len(username) > 64 || len(password) > 128 {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    // Allow only a safe username character set to avoid weird inputs.
    validUser := regexp.MustCompile(`^[A-Za-z0-9_]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Constant-time comparison for username to reduce timing attack surface.
    // Normalize to the exact stored form: we compare raw bytes.
    if subtle.ConstantTimeCompare([]byte(username), []byte(modUsername)) != 1 {
        respondJSON(w, map[string]bool{"moderator": false})
        return
    }

    // Compare password using bcrypt (secure hash check). Stored value must be a bcrypt hash.
    if err := bcrypt.CompareHashAndPassword([]byte(modPasswordHash), []byte(password)); err != nil {
        // On any error, treat as non-moderator without leaking details.
        respondJSON(w, map[string]bool{"moderator": false})
        return
    }

    // Successful authentication => moderator.
    respondJSON(w, map[string]bool{"moderator": true})
}

// respondJSON writes a safe JSON response and sets content-type.
// It never reflects raw user input.
func respondJSON(w http.ResponseWriter, v interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // No pretty printing to avoid accidental data exposure; encoder escapes as needed.
    enc := json.NewEncoder(w)
    _ = enc.Encode(v)
}

// registerAttempt returns true if the IP should be blocked.
func registerAttempt(ip string) bool {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()

    now := time.Now()
    a, ok := attempts[ip]
    if !ok {
        attempts[ip] = &ipAttempt{count: 1, firstStamp: now}
        return false
    }

    // Reset window after 1 minute.
    if now.Sub(a.firstStamp) > time.Minute {
        a.count = 1
        a.firstStamp = now
        return false
    }

    a.count++
    // Block if more than 10 attempts in the window.
    if a.count > 10 {
        return true
    }
    return false
}
// ...existing code...
