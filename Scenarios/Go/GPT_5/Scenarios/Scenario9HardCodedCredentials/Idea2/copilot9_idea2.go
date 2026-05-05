package main

import (
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
var (
    // Precompute a dummy bcrypt hash used to equalize timing when a user is not configured.
    // This prevents timing-based username enumeration.
    dummyHash []byte
)

func init() {
    var err error
    // Use MinCost here so startup is fast; CompareHashAndPassword still has consistent timing properties.
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("dummy_timing_secret"), bcrypt.MinCost)
    if err != nil {
        log.Fatalf("failed to initialize dummy bcrypt hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // Do not log or print secrets. Keep the listen address configurable in production.
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Allow only GET for this endpoint (matches current usage). Change to POST if sending credentials in body.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Minimal input validation and sanitisation.
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Validate username: allow letters, digits, underscore and hyphen, and sensible length.
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]{3,30}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    // Validate password length.
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    // Obtain stored bcrypt hash for the moderator from environment.
    // Production: set environment variables like MOD_HASH_alice="<bcrypt-hash>"
    envKey := "MOD_HASH_" + username
    stored := os.Getenv(envKey)

    // Use dummyHash if the user is not configured to avoid leaking whether a username exists via timing.
    targetHash := dummyHash
    userConfigured := false
    if stored != "" {
        targetHash = []byte(stored)
        userConfigured = true
    }

    // Compare password with bcrypt. bcrypt.CompareHashAndPassword is safe for password verification.
    err := bcrypt.CompareHashAndPassword(targetHash, []byte(password))

    isModerator := err == nil && userConfigured

    // Security headers and JSON response. Do not echo back username or password.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Cache-Control", "no-store")

    resp := struct {
        IsModerator bool `json:"is_moderator"`
    }{
        IsModerator: isModerator,
    }

    // Always return 200 with a JSON body to avoid leaking details through status codes.
    w.WriteHeader(http.StatusOK)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // extra safeguard against XSS if any fields changed in future
    _ = enc.Encode(resp)
}
// ...existing code...
