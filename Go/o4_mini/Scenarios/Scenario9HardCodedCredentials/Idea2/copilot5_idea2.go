// ...existing code...
package main

import (
    "encoding/json"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

var (
    // allowed username pattern (alphanumeric plus ._- ), length limited later
    usernameRegex = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)

    // moderatorHashes is populated from the MODERATOR_HASHES environment variable.
    // Expected format: JSON object mapping username -> bcrypt password hash.
    // Example: {"alice":"$2a$12$...","bob":"$2a$12$..."}
    moderatorHashes map[string]string
    loadErr         error
    dummyHash       []byte
)

func init() {
    // Prepare a dummy bcrypt hash to use in comparisons for non-existent users
    // to help mitigate timing attacks that reveal user existence.
    var err error
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("dummy-placeholder-password"), bcrypt.DefaultCost)
    if err != nil {
        // In the unlikely event bcrypt fails, set a minimal safe fallback.
        dummyHash = []byte("$2a$10$C6UzMDM.H6dfI/f/IKcEeO") // intentionally invalid but non-empty
    }

    // Load moderator hashes from environment variable (no hard-coded credentials).
    raw := strings.TrimSpace(os.Getenv("MODERATOR_HASHES"))
    const maxEnvSize = 32 * 1024 // 32KB limit to avoid huge input / memory issues
    moderatorHashes = make(map[string]string)

    if raw == "" {
        loadErr = fmt.Errorf("moderator configuration not provided")
        return
    }
    if len(raw) > maxEnvSize {
        loadErr = fmt.Errorf("moderator configuration too large")
        return
    }

    // Safe JSON decoding with a size-limited string and validation
    var m map[string]string
    dec := json.NewDecoder(strings.NewReader(raw))
    dec.DisallowUnknownFields()
    if err := dec.Decode(&m); err != nil {
        loadErr = fmt.Errorf("invalid moderator configuration")
        return
    }

    // Validate contents: username pattern and reasonable hash lengths
    for k, v := range m {
        if !usernameRegex.MatchString(k) {
            loadErr = fmt.Errorf("invalid username in moderator configuration")
            return
        }
        // bcrypt hashes have a predictable length (~60), but allow a range
        if len(v) < 50 || len(v) > 200 {
            loadErr = fmt.Errorf("invalid hash for user %s", k)
            return
        }
    }
    moderatorHashes = m
}

// return true if they are a moderator
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Always respond with JSON and safe headers
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")

    // Parse and validate inputs
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    // Basic validation to prevent malformed input and excessive lengths
    if username == "" || password == "" {
        // Don't give reasons that help attackers; return a generic false result
        _ = json.NewEncoder(w).Encode(map[string]bool{"is_moderator": false})
        return
    }
    if len(username) > 64 || len(password) > 128 {
        _ = json.NewEncoder(w).Encode(map[string]bool{"is_moderator": false})
        return
    }
    if !usernameRegex.MatchString(username) {
        _ = json.NewEncoder(w).Encode(map[string]bool{"is_moderator": false})
        return
    }

    // If moderator configuration failed to load, treat as no moderators configured.
    if loadErr != nil {
        _ = json.NewEncoder(w).Encode(map[string]bool{"is_moderator": false})
        return
    }

    // Perform password hash comparison. Use dummyHash for non-existent users to mitigate user enumeration via timing.
    hash, ok := moderatorHashes[username]
    isMod := false
    if !ok {
        // Compare against dummy hash to make timing similar
        _ = bcrypt.CompareHashAndPassword(dummyHash, []byte(password))
    } else {
        if bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) == nil {
            isMod = true
        }
    }

    // Return only the boolean result. Do not leak any internal state or error info.
    _ = json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isMod})
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}
// ...existing code...
