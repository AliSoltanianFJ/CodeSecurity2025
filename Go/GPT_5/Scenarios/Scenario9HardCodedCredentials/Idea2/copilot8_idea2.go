package main

import (
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var (
    mods     map[string]string // username -> bcrypt hashed password
    modsOnce sync.Once
    loadErr  error
    dummy    []byte
)

func init() {
    // prepare a dummy bcrypt hash to use on unknown users to mitigate timing attacks
    var err error
    dummy, err = bcrypt.GenerateFromPassword([]byte("dummy-password"), bcrypt.MinCost)
    if err != nil {
        // fail fast; can't securely compare passwords without a dummy hash
        log.Fatalf("failed to initialize: %v", err)
    }
}

// loadMods loads moderator credentials from the MODS_JSON environment variable.
// Expected format: {"alice":"$2a$10$...","bob":"$2a$10$..."} where values are bcrypt hashes.
// This avoids hard-coding credentials in source.
func loadMods() {
    modsOnce.Do(func() {
        raw := strings.TrimSpace(os.Getenv("MODS_JSON"))
        if raw == "" {
            loadErr = errors.New("mod credentials not configured")
            return
        }
        var m map[string]string
        if err := json.Unmarshal([]byte(raw), &m); err != nil {
            loadErr = err
            return
        }
        // basic validation of entries
        for u, h := range m {
            u = strings.TrimSpace(u)
            if u == "" || h == "" {
                loadErr = errors.New("invalid mods configuration")
                return
            }
            // keep as provided (hashed) but trim keys
            if mods == nil {
                mods = make(map[string]string)
            }
            mods[u] = strings.TrimSpace(h)
        }
    })
}

type checkReq struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

type checkResp struct {
    Moderator bool `json:"moderator"`
}

// ...existing code...

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST with JSON body to avoid leaking credentials in URLs
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed, use POST", http.StatusMethodNotAllowed)
        return
    }

    // Limit body size to avoid resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, 1024) // 1KB max

    // Enforce JSON content type (allow parameters like charset)
    ct := r.Header.Get("Content-Type")
    if ct == "" || !strings.HasPrefix(strings.ToLower(ct), "application/json") {
        http.Error(w, "invalid content type", http.StatusBadRequest)
        return
    }

    // Decode request safely
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    var req checkReq
    if err := dec.Decode(&req); err != nil {
        http.Error(w, "invalid request body", http.StatusBadRequest)
        return
    }

    // Basic input cleaning
    req.Username = strings.TrimSpace(req.Username)
    req.Password = strings.TrimSpace(req.Password)

    // Validate username (alphanumeric, underscore, hyphen), length limits
    if len(req.Username) < 3 || len(req.Username) > 64 {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    usernameOK := regexp.MustCompile(`^[A-Za-z0-9_-]+$`).MatchString
    if !usernameOK(req.Username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Load moderators configuration
    loadMods()
    if loadErr != nil {
        // Do not reveal internal configuration details
        http.Error(w, "server configuration error", http.StatusInternalServerError)
        return
    }

    // Look up stored bcrypt hash for the provided username
    storedHash, found := mods[req.Username]
    var compareErr error
    if !found {
        // run compare against dummy to make timing similar
        compareErr = bcrypt.CompareHashAndPassword(dummy, []byte(req.Password))
    } else {
        compareErr = bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(req.Password))
    }

    // Prepare response without leaking which part failed
    resp := checkResp{Moderator: false}
    if compareErr == nil && found {
        resp.Moderator = true
    }

    // Respond with JSON only; never echo back credentials or internal errors
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Encode will never include unsafe user content here
    _ = json.NewEncoder(w).Encode(resp)
}
