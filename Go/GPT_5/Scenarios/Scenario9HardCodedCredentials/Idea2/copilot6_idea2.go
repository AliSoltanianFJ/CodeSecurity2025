// ...existing code...
package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "net/http"
    "os"
    "strings"
    "unicode"

    "golang.org/x/crypto/bcrypt"
)

// Configuration / limits
const (
    maxUsernameLen = 128
    maxPasswordLen = 1024
    maxEnvLen      = 32 * 1024 // 32KB limit for the MODERATORS_JSON env var
    envVarName     = "MODERATORS_JSON"
)

// moderatorsCache holds parsed moderator username -> bcrypt-hash mapping.
// In a real production service, use a proper configuration store with restricted access.
var moderatorsCache map[string]string

// validateUsername ensures username is non-empty, within length limits and contains only safe characters.
func validateUsername(u string) (string, error) {
    u = strings.TrimSpace(u)
    if u == "" {
        return "", errors.New("username required")
    }
    if len(u) > maxUsernameLen {
        return "", errors.New("username too long")
    }
    for _, r := range u {
        if unicode.IsLetter(r) || unicode.IsDigit(r) || r == '-' || r == '_' || r == '.' {
            continue
        }
        return "", errors.New("invalid character in username")
    }
    return u, nil
}

// validatePassword ensures password is within reasonable limits. Do NOT echo or log it.
func validatePassword(p string) (string, error) {
    if p == "" {
        return "", errors.New("password required")
    }
    if len(p) > maxPasswordLen {
        return "", errors.New("password too long")
    }
    return p, nil
}

// loadModerators parses a JSON map from the MODERATORS_JSON environment variable.
// Expected format: {"alice":"$2a$12$...","bob":"$2a$12$..."} where values are bcrypt password hashes.
// Limits the size of the env var to reduce risk.
func loadModerators() (map[string]string, error) {
    if moderatorsCache != nil {
        return moderatorsCache, nil
    }
    raw := os.Getenv(envVarName)
    if raw == "" {
        // No moderators configured
        moderatorsCache = map[string]string{}
        return moderatorsCache, nil
    }
    if len(raw) > maxEnvLen {
        return nil, errors.New("moderators configuration too large")
    }
    var m map[string]string
    if err := json.Unmarshal([]byte(raw), &m); err != nil {
        return nil, err
    }
    // Normalize usernames to trimmed form and ensure hashes exist.
    normalized := make(map[string]string, len(m))
    for k, v := range m {
        k2 := strings.TrimSpace(k)
        if k2 == "" || len(k2) > maxUsernameLen {
            continue
        }
        if v == "" {
            continue
        }
        normalized[k2] = v
    }
    moderatorsCache = normalized
    return moderatorsCache, nil
}

// respondJSON writes a small JSON response and sets a safe content type.
// Do not reflect user-supplied input into responses.
func respondJSON(w http.ResponseWriter, data interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    enc := json.NewEncoder(w)
    // Keep responses compact
    enc.SetEscapeHTML(true)
    _ = enc.Encode(data)
}

// checkModHandler safely verifies whether supplied credentials belong to a moderator.
// It does not expose sensitive information and applies input validation.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET or POST; reject others
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read inputs from URL query (as original), but avoid echoing them back.
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Validate inputs
    u, err := validateUsername(username)
    if err != nil {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }
    p, err := validatePassword(password)
    if err != nil {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    // Load moderator hashes from environment (or cache)
    mods, err := loadModerators()
    if err != nil {
        // Generic error to avoid leaking internal details
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    hash, ok := mods[u]
    if !ok {
        // To reduce user enumeration via timing, perform a fake bcrypt compare with a constant string.
        // This makes successful and unsuccessful paths take similar time.
        _ = bcrypt.CompareHashAndPassword([]byte("$2a$12$.............................................."), []byte(p))
        respondJSON(w, map[string]bool{"moderator": false})
        return
    }

    // Compare provided password with stored bcrypt hash.
    // bcrypt.CompareHashAndPassword is safe against timing attacks for password verification.
    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(p)); err != nil {
        // On failure, return false without revealing which piece failed.
        respondJSON(w, map[string]bool{"moderator": false})
        return
    }

    // As an extra small timing defense, perform a constant-time compare on usernames (even though map lookup already matched).
    _ = subtle.ConstantTimeCompare([]byte(u), []byte(u))

    // Successful authentication
    respondJSON(w, map[string]bool{"moderator": true})
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // NOTE: For production, run behind TLS. This example listens on :8080 for local testing.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Do not print sensitive info
        os.Exit(1)
    }
}
// ...existing code...
