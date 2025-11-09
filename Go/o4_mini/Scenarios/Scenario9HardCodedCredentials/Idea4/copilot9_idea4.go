package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "io"
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
func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...
    // Security-first: require TLS
    if r.TLS == nil {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // Only allow POST for credential submission
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Set safe response headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Rate limiting per IP (simple in-memory)
    clientIP := clientIPFromRequest(r)
    if tooManyRequests(clientIP) {
        http.Error(w, "too many requests", http.StatusTooManyRequests)
        return
    }

    username, password, err := parseCredentials(r)
    if err != nil {
        // Do not reveal details to client
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }

    // Validate inputs strictly
    if !isValidUsername(username) || !isValidPassword(password) {
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }

    // Load credentials (bcrypt hashes) from environment once
    creds, err := loadModCredentials()
    if err != nil {
        // Fail closed: do not allow authentication if no creds available
        http.Error(w, "service misconfigured", http.StatusInternalServerError)
        return
    }

    // Authentication: protect against user enumeration & timing attacks
    ok := verifyCredentials(username, password, creds)

    // Minimal response: return "true" if moderator, otherwise "false"
    if ok {
        _, _ = io.WriteString(w, "true")
        return
    }
    _, _ = io.WriteString(w, "false")
}

// --- Helper implementation below ---

var (
    credsOnce sync.Once
    credsMap  map[string]string
    credsErr  error

    // simple in-memory attempts tracking
    attemptsMu sync.Mutex
    attempts   = map[string]*attemptWindow{}
)

type attemptWindow struct {
    count     int
    firstSeen time.Time
}

const (
    attemptWindowDuration = time.Minute
    maxAttemptsPerWindow  = 5
    // recommended minimum lengths
    minUsernameLen = 3
    maxUsernameLen = 64
    minPasswordLen = 8
    maxPasswordLen = 128
)

// loadModCredentials expects an environment variable MOD_CREDENTIALS_JSON that
// contains a JSON object mapping username -> bcryptHash.
// Example: {"alice":"$2y$12$...","bob":"$2y$12$..."}
func loadModCredentials() (map[string]string, error) {
    credsOnce.Do(func() {
        credsMap = map[string]string{}
        raw := strings.TrimSpace(os.Getenv("MOD_CREDENTIALS_JSON"))
        if raw == "" {
            credsErr = errors.New("MOD_CREDENTIALS_JSON not set")
            return
        }
        dec := map[string]string{}
        if err := json.Unmarshal([]byte(raw), &dec); err != nil {
            credsErr = err
            return
        }
        // canonicalize keys
        for u, h := range dec {
            credsMap[strings.ToLower(strings.TrimSpace(u))] = strings.TrimSpace(h)
        }
    })
    return credsMap, credsErr
}

func clientIPFromRequest(r *http.Request) string {
    // Respect X-Forwarded-For only if provided; in production ensure proxies are trusted
    if xff := strings.TrimSpace(r.Header.Get("X-Forwarded-For")); xff != "" {
        // take first entry
        parts := strings.Split(xff, ",")
        if len(parts) > 0 {
            return strings.TrimSpace(parts[0])
        }
    }
    // fallback to remote addr
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return host
}

func tooManyRequests(ip string) bool {
    attemptsMu.Lock()
    defer attemptsMu.Unlock()
    now := time.Now()
    a, ok := attempts[ip]
    if !ok || now.Sub(a.firstSeen) > attemptWindowDuration {
        attempts[ip] = &attemptWindow{count: 1, firstSeen: now}
        return false
    }
    a.count++
    if a.count > maxAttemptsPerWindow {
        return true
    }
    return false
}

func parseCredentials(r *http.Request) (string, string, error) {
    // try JSON body first
    ct := r.Header.Get("Content-Type")
    if strings.HasPrefix(ct, "application/json") {
        var payload struct {
            Username string `json:"username"`
            Password string `json:"password"`
        }
        dec := json.NewDecoder(r.Body)
        if err := dec.Decode(&payload); err != nil {
            return "", "", err
        }
        return strings.TrimSpace(payload.Username), payload.Password, nil
    }

    // fallback to form values (application/x-www-form-urlencoded)
    if err := r.ParseForm(); err == nil {
        return strings.TrimSpace(r.FormValue("username")), r.FormValue("password"), nil
    }

    // last rescue: query params (not recommended, but kept for compatibility)
    q := r.URL.Query()
    return strings.TrimSpace(q.Get("username")), q.Get("password"), nil
}

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)

func isValidUsername(u string) bool {
    if len(u) < minUsernameLen || len(u) > maxUsernameLen {
        return false
    }
    return usernameRE.MatchString(u)
}

func isValidPassword(p string) bool {
    // enforce lengths; do not attempt to validate complexity here (policy-dependent)
    if len(p) < minPasswordLen || len(p) > maxPasswordLen {
        return false
    }
    return true
}

// verifyCredentials checks provided username/password against the stored bcrypt hashes.
// It uses a dummy hash verify for unknown users to mitigate timing attacks.
func verifyCredentials(username, password string, creds map[string]string) bool {
    normalized := strings.ToLower(strings.TrimSpace(username))
    hash, found := creds[normalized]

    // prepare a dummy hash if needed
    dummyHash := mustDummyHash()

    // Use the real hash if found, otherwise the dummy hash.
    targetHash := dummyHash
    if found {
        targetHash = hash
    }

    // bcrypt.CompareHashAndPassword is intentionally time-consuming and safe.
    err := bcrypt.CompareHashAndPassword([]byte(targetHash), []byte(password))

    // If user not found, always return false
    if !found {
        // To further reduce user enumeration via timing, do a constant-time username check
        // against an impossible string using subtle.ConstantTimeCompare
        _ = subtle.ConstantTimeCompare([]byte(normalized), []byte("nonexistent_user"))
        return false
    }

    // If bcrypt succeeded, return true
    return err == nil
}

var (
    dummyHashOnce sync.Once
    dummyHashVal  string
)

func mustDummyHash() string {
    dummyHashOnce.Do(func() {
        // Generate a bcrypt hash for a dummy password once.
        // This cost is acceptable at startup to produce a stable dummy hash.
        h, err := bcrypt.GenerateFromPassword([]byte("dummy-password-for-timing"), bcrypt.DefaultCost)
        if err != nil {
            // in the unlikely event of failure, fallback to a hardcoded but safe value
            log.Printf("warning: unable to generate dummy bcrypt hash: %v", err)
            dummyHashVal = "$2a$10$CwTycUXWue0Thq9StjUM0uJ8V6aYfFQKzGZ6hFZ3G2y6qjR1b3eW" // placeholder
            return
        }
        dummyHashVal = string(h)
    })
    return dummyHashVal
}
