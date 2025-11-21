// ...existing code...
package main

import (
    "encoding/json"
    "errors"
    "fmt"
    "net"
    "net/http"
    "os"
    "regexp"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    modHashes   map[string]string // username -> bcrypt hash
    loadOnce    sync.Once
    loadErr     error
    fakeHash    []byte
    attemptsMu  sync.Mutex
    attemptsMap = make(map[string][]time.Time) // ip -> timestamps
)

// loadModerators loads moderator bcrypt hashes from a JSON file path given by MODS_FILE env var.
// The file must contain a JSON object mapping usernames to bcrypt hashes: {"alice":"$2a$...","bob":"$2a$..."}
func loadModerators() {
    loadOnce.Do(func() {
        // Prepare a fake hash to use for constant-time compare when user doesn't exist.
        h, err := bcrypt.GenerateFromPassword([]byte("invalid-placeholder-password"), bcrypt.MinCost)
        if err != nil {
            // fallback to a hard-coded valid bcrypt hash (non-secret)
            fakeHash = []byte("$2a$10$CwTycUXWue0Thq9StjUM0uJ8qW8g1/0q4aMZ/ED8q8u9qKqv7rGFa") // hashed "password"
        } else {
            fakeHash = h
        }

        path := os.Getenv("MODS_FILE")
        if path == "" {
            loadErr = errors.New("configuration error: MODS_FILE environment variable not set")
            return
        }
        f, err := os.Open(path)
        if err != nil {
            loadErr = fmt.Errorf("cannot open mods file: %w", err)
            return
        }
        defer f.Close()

        dec := json.NewDecoder(f)
        var data map[string]string
        if err := dec.Decode(&data); err != nil {
            loadErr = fmt.Errorf("cannot parse mods file: %w", err)
            return
        }
        // Basic validation of loaded hashes
        for u, h := range data {
            if u == "" || len(u) > 64 {
                loadErr = fmt.Errorf("invalid username in mods file")
                return
            }
            if len(h) < 20 { // rough check; bcrypt hashes are ~60 chars
                loadErr = fmt.Errorf("invalid hash for user %s", u)
                return
            }
        }
        modHashes = data
    })
}

// rateLimit checks and records an attempt from the given IP.
// Returns true if allowed, false if rate limit exceeded.
func rateLimit(ip string) bool {
    const window = time.Minute
    const maxAttempts = 5

    now := time.Now()
    attemptsMu.Lock()
    defer attemptsMu.Unlock()

    times := attemptsMap[ip]
    // prune old entries
    pruned := times[:0]
    for _, t := range times {
        if now.Sub(t) <= window {
            pruned = append(pruned, t)
        }
    }
    pruned = append(pruned, now)
    attemptsMap[ip] = pruned
    return len(pruned) <= maxAttempts
}

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // Don't expose server errors or stack traces
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Fprintln(os.Stderr, "server error:", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Load moderators once
    loadModerators()
    if loadErr != nil {
        http.Error(w, "server configuration error", http.StatusInternalServerError)
        return
    }

    // Only accept GET for this endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic rate limiting by remote IP (prevents brute force)
    ip, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        // fallback to whole RemoteAddr if SplitHostPort fails
        ip = r.RemoteAddr
    }
    if !rateLimit(ip) {
        http.Error(w, "too many requests", http.StatusTooManyRequests)
        return
    }

    // Read and validate inputs
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Validate presence and reasonable length
    if username == "" || password == "" {
        http.Error(w, "missing credentials", http.StatusBadRequest)
        return
    }
    if len(password) > 128 {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }
    if !usernameRE.MatchString(username) {
        // do not provide details that aid enumeration
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    // Lookup hash. If not found, use fakeHash to make timing similar and avoid user enumeration.
    hash, found := modHashes[username]
    var hashToCheck []byte
    if found {
        hashToCheck = []byte(hash)
    } else {
        hashToCheck = fakeHash
    }

    // Compare using bcrypt which is resistant to timing attacks for password checks.
    err = bcrypt.CompareHashAndPassword(hashToCheck, []byte(password))
    isMod := (err == nil && found) // only true if password matched and user existed

    // Respond with JSON only; never reflect inputs or internal errors back to the client.
    resp := struct {
        IsModerator bool `json:"is_moderator"`
    }{
        IsModerator: isMod,
    }

    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // extra safe; though we don't output raw user data
    if err := enc.Encode(resp); err != nil {
        // If encoding fails, return a generic error
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
}
