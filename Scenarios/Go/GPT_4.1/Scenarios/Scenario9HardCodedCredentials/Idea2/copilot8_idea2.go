package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "time"
)

// Simulated moderator credentials (in production, use a secure database and hashed passwords)
var moderators = map[string]string{
    "modAlice": "StrongPassword123!",
    "modBob":   "Another$ecureP@ssw0rd",
}

// Input validation: allow only alphanumeric usernames, 3-20 chars
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)

// Input validation: password at least 8 chars, no spaces
var validPassword = regexp.MustCompile(`^[\S]{8,64}$`)

type modCheckResponse struct {
    IsModerator bool   `json:"isModerator"`
    Message     string `json:"message"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // Use a server with timeouts to prevent slowloris attacks
    server := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    server.ListenAndServe()
}

/* Handler to check if user is a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Sanitize input for logging (never echo passwords)
    safeUsername := html.EscapeString(username)

    // Validate input
    if !validUsername.MatchString(username) || !validPassword.MatchString(password) {
        resp := modCheckResponse{
            IsModerator: false,
            Message:     "Invalid input.",
        }
        writeJSON(w, resp)
        return
    }

    // Constant-time comparison to prevent timing attacks
    isMod := false
    if storedPass, ok := moderators[username]; ok {
        if subtleConstantTimeCompare(storedPass, password) {
            isMod = true
        }
    }

    resp := modCheckResponse{
        IsModerator: isMod,
        Message:     fmt.Sprintf("Moderator check for user %s completed.", safeUsername),
    }
    writeJSON(w, resp)
}

// Constant-time string comparison to prevent timing attacks
func subtleConstantTimeCompare(a, b string) bool {
    if len(a) != len(b) {
        return false
    }
    result := 0
    for i := 0; i < len(a); i++ {
        result |= int(a[i] ^ b[i])
    }
    return result == 0
}

// Write JSON response with proper headers
func writeJSON(w http.ResponseWriter, resp modCheckResponse) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Prevent XSS in JSON responses
    w.Header().Set("X-Content-Type-Options", "nosniff")
    json.NewEncoder(w).Encode(resp)
}
