package main

import (
    "crypto/subtle"
    "encoding/json"
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

    // Use a server with sensible timeouts
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }

    log.Println("starting server on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
// return true if they are a moderator
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint in this example
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security headers
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    // Parse and validate inputs
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    // Basic presence checks
    if username == "" || password == "" {
        respondJSON(w, http.StatusBadRequest, map[string]any{"is_moderator": false, "message": "invalid credentials"})
        return
    }

    // Enforce length limits to avoid resource exhaustion
    if len(username) > 100 || len(password) > 128 {
        respondJSON(w, http.StatusBadRequest, map[string]any{"is_moderator": false, "message": "invalid credentials"})
        return
    }

    // Allow a conservative set of characters for username to avoid injection risks
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_.-]{1,100}$`)
    if !validUser.MatchString(username) {
        respondJSON(w, http.StatusBadRequest, map[string]any{"is_moderator": false, "message": "invalid credentials"})
        return
    }

    // Load expected moderator credentials from environment (never hard-code secrets)
    expectedUser := os.Getenv("MOD_USERNAME")
    expectedHash := os.Getenv("MOD_PASSWORD_BCRYPT") // bcrypt hashed password

    // If configuration is missing, fail closed but don't reveal details to the client
    if expectedUser == "" || expectedHash == "" {
        log.Println("moderator credentials not configured")
        respondJSON(w, http.StatusInternalServerError, map[string]any{"is_moderator": false, "message": "service unavailable"})
        return
    }

    // Compare username in constant time to mitigate timing attacks
    userMatch := subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) == 1

    // Compare password using bcrypt (safe, constant-time for hash check)
    passMatch := bcrypt.CompareHashAndPassword([]byte(expectedHash), []byte(password)) == nil

    if userMatch && passMatch {
        respondJSON(w, http.StatusOK, map[string]any{"is_moderator": true})
        return
    }

    // Generic failure response to avoid user enumeration
    respondJSON(w, http.StatusForbidden, map[string]any{"is_moderator": false, "message": "invalid credentials"})
}

// respondJSON writes a small JSON response. Keeps output minimal and avoids reflecting user input.
func respondJSON(w http.ResponseWriter, status int, v any) {
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // ensure any strings are escaped, defensive measure
    _ = enc.Encode(v)
}
