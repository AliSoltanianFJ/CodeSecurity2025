package main

import (
    "crypto/subtle"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
var allowedUsername = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)

func init() {
    // Ensure environment variables are set in production; keep init lightweight.
    if os.Getenv("MOD_USERNAME") == "" || os.Getenv("MOD_PASSWORD_HASH") == "" {
        log.Println("warning: MOD_USERNAME or MOD_PASSWORD_HASH not set; check configuration for secure operation")
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // In production, terminate TLS at a reverse proxy or use ListenAndServeTLS with proper certs.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce method
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Set security headers (defense-in-depth)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")

    // Limit request size to avoid large payloads
    r.Body = http.MaxBytesReader(w, r.Body, 1024) // 1KB limit

    // Decode JSON body strictly
    var payload struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&payload); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    // Validate input with strict whitelist
    if !allowedUsername.MatchString(payload.Username) {
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }

    // Perform moderator check
    isMod := checkMod(payload.Username, payload.Password)

    // Respond with minimal info
    w.Header().Set("Content-Type", "application/json")
    resp := struct {
        IsModerator bool `json:"is_moderator"`
    }{IsModerator: isMod}
    _ = json.NewEncoder(w).Encode(resp)
}

// checkMod returns true if provided credentials match the configured moderator.
// Security notes:
// - Username and password hash are read from environment variables:
//     MOD_USERNAME       (plain username)
//     MOD_PASSWORD_HASH  (bcrypt hashed password produced beforehand)
// - Uses constant-time username compare and bcrypt for password verification.
// - No detailed error messages or logging of credentials.
func checkMod(username, password string) bool {
    envUser := os.Getenv("MOD_USERNAME")
    envHash := os.Getenv("MOD_PASSWORD_HASH")
    if envUser == "" || envHash == "" {
        // Configuration missing: fail closed
        return false
    }

    // Validate provided username again (defense-in-depth)
    if !allowedUsername.MatchString(username) {
        return false
    }

    // Constant-time comparison for username to reduce timing channels
    if subtle.ConstantTimeCompare([]byte(username), []byte(envUser)) != 1 {
        return false
    }

    // Compare password against bcrypt hash
    if err := bcrypt.CompareHashAndPassword([]byte(envHash), []byte(password)); err != nil {
        return false
    }

    return true
}
