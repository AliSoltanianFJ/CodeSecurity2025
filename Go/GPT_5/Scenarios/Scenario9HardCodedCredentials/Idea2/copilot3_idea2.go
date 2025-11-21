// ...existing code...
package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var (
    // compiled once for input validation
    usernameRE = regexp.MustCompile(`^[A-Za-z0-9_-]{3,64}$`)

    // a fake bcrypt hash used to ensure password checks take similar time
    // even when username is missing; generated at init.
    fakeHash []byte
)

func init() {
    // generate a fake bcrypt hash on startup (cost = bcrypt.DefaultCost)
    // used to mitigate username enumeration via timing attacks.
    h, err := bcrypt.GenerateFromPassword([]byte("invalid-password"), bcrypt.DefaultCost)
    if err != nil {
        // if bcrypt generation fails, fallback to a stable precomputed hash (valid bcrypt format)
        // This branch should rarely happen; do not expose details.
        fakeHash = []byte("$2a$10$N9qo8uLOickgx2ZMRZo5i.uWqY6K/1f1K6jZ4Y1m7w6q6Wq1f1uK") // still a placeholder valid-format
    } else {
        fakeHash = h
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  30 * time.Second,
    }
    if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
        fmt.Println("server error:", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST to avoid credentials in URLs/logs
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit size of request body to avoid resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, 2048) // 2 KiB should be plenty for JSON creds

    // Expect JSON body with explicit fields; disallow unknown fields to avoid unexpected input
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()

    var payload struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    if err := dec.Decode(&payload); err != nil {
        http.Error(w, "invalid request body", http.StatusBadRequest)
        return
    }

    // Basic validation / sanitization
    username := payload.Username
    password := payload.Password

    if !usernameRE.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    // Retrieve expected credentials from environment variables.
    // Do NOT hardcode credentials in source.
    expectedUser := os.Getenv("MOD_USERNAME")
    expectedHash := os.Getenv("MOD_PASSWORD_HASH") // must be a bcrypt hash

    // Use constant-time comparison for username to reduce timing side-channels.
    usernameMatch := 0
    if expectedUser != "" {
        // compare lengths first in constant-time manner by using subtle.ConstantTimeCompare
        usernameMatch = subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser))
    }

    // choose which hash to compare to: the real one (if username matches and provided)
    // otherwise use the fake hash to keep timing consistent.
    hashToCheck := fakeHash
    if usernameMatch == 1 && expectedHash != "" {
        hashToCheck = []byte(expectedHash)
    }

    // Compare password with bcrypt. Do not reveal whether username or password was wrong.
    if err := bcrypt.CompareHashAndPassword(hashToCheck, []byte(password)); err != nil {
        // authentication failed
        respondJSON(w, http.StatusOK, map[string]bool{"moderator": false})
        return
    }

    // If we reached here and username matched, authentication succeeded.
    // If username didn't match but bcrypt happened to match fakeHash (very unlikely),
    // we still treat as non-moderator unless username matched.
    if usernameMatch != 1 {
        respondJSON(w, http.StatusOK, map[string]bool{"moderator": false})
        return
    }

    respondJSON(w, http.StatusOK, map[string]bool{"moderator": true})
}

// respondJSON writes JSON response with proper content type and without exposing sensitive details.
func respondJSON(w http.ResponseWriter, status int, v interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // extra XSS safety if values somehow contain html
    _ = enc.Encode(v)
}
