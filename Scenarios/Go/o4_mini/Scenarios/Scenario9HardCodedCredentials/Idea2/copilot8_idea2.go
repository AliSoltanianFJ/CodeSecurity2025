package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/check_mod", checkModHandler)

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
        Handler:      nil,
    }

    log.Println("starting server on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST with JSON body
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security headers
    w.Header().Set("Content-Type", "application/json")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    // Decode JSON safely, disallow unknown fields to prevent insecure deserialization
    type credentials struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    var cred credentials
    if err := dec.Decode(&cred); err != nil {
        http.Error(w, `{"is_moderator":false}`, http.StatusBadRequest)
        return
    }
    // ensure no further data
    if dec.More() {
        http.Error(w, `{"is_moderator":false}`, http.StatusBadRequest)
        return
    }

    // Basic sanitisation and validation
    cred.Username = strings.TrimSpace(cred.Username)
    cred.Password = strings.TrimSpace(cred.Password)

    if err := validateInput(cred.Username, cred.Password); err != nil {
        // Do not reveal which check failed
        http.Error(w, `{"is_moderator":false}`, http.StatusBadRequest)
        return
    }

    ok, err := isModerator(cred.Username, cred.Password)
    if err != nil {
        // Internal error — do not leak details
        http.Error(w, `{"is_moderator":false}`, http.StatusInternalServerError)
        return
    }

    // Respond with JSON only; do not include any sensitive information
    resp := struct {
        IsModerator bool `json:"is_moderator"`
    }{
        IsModerator: ok,
    }
    _ = json.NewEncoder(w).Encode(resp)
}

// validateInput applies conservative checks to inputs
func validateInput(username, password string) error {
    // Username: allow letters, digits, dot, underscore, hyphen, length 3..64
    uRe := regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)
    if !uRe.MatchString(username) {
        return errors.New("invalid username")
    }

    // Password: enforce reasonable length only; do not attempt to validate contents further
    if len(password) < 8 || len(password) > 128 {
        return errors.New("invalid password length")
    }
    return nil
}

// isModerator checks credentials against environment-secured values.
// Required environment variables:
//   MOD_USERNAME - the canonical moderator username
//   MOD_PASSWORD_HASH - bcrypt hash of the moderator password
func isModerator(username, password string) (bool, error) {
    modUser := os.Getenv("MOD_USERNAME")
    modHash := os.Getenv("MOD_PASSWORD_HASH")

    // Fail closed if configuration missing
    if modUser == "" || modHash == "" {
        log.Println("moderator credentials not configured")
        return false, errors.New("configuration error")
    }

    // Compare username in constant time
    if len(username) != len(modUser) {
        // still perform a constant-time comparison on equal length slices to mitigate timing
        _ = subtle.ConstantTimeCompare([]byte(username), []byte(modUser))
        return false, nil
    }
    if subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) != 1 {
        return false, nil
    }

    // Compare password using bcrypt (safe, constant-time internal checks)
    if err := bcrypt.CompareHashAndPassword([]byte(modHash), []byte(password)); err != nil {
        // incorrect password
        return false, nil
    }

    // On success, return true
    return true, nil
}
