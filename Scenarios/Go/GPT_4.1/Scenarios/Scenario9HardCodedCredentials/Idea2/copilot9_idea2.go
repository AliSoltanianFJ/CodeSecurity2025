package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
    "time"
)

// Simulated user store (in production, use a secure database and hashed passwords)
var moderators = map[string]string{
    "moduser1": "StrongPassword123!",
    "moduser2": "Another$ecureP@ss",
}

// Input validation: allow only alphanumeric usernames, 3-32 chars
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

// Response structure
type modCheckResponse struct {
    IsModerator bool   `json:"is_moderator"`
    Message     string `json:"message"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // Use a server with timeouts to prevent Slowloris attacks
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    srv.ListenAndServe()
}

/* Handler to check if user is a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size (defensive, though GET should have no body)
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    // Sanitize input for HTML context (defense-in-depth)
    username = html.EscapeString(username)

    // Validate username
    if !validUsername.MatchString(username) {
        writeJSON(w, http.StatusBadRequest, modCheckResponse{
            IsModerator: false,
            Message:     "Invalid username format.",
        })
        return
    }

    // Validate password length (defensive, don't leak info)
    if len(password) < 8 || len(password) > 64 {
        writeJSON(w, http.StatusUnauthorized, modCheckResponse{
            IsModerator: false,
            Message:     "Invalid credentials.",
        })
        return
    }

    // Constant-time comparison to prevent timing attacks
    if storedPass, ok := moderators[username]; ok && secureCompare(storedPass, password) {
        writeJSON(w, http.StatusOK, modCheckResponse{
            IsModerator: true,
            Message:     "User is a moderator.",
        })
        return
    }

    writeJSON(w, http.StatusUnauthorized, modCheckResponse{
        IsModerator: false,
        Message:     "Invalid credentials.",
    })
}

// Constant-time string comparison
func secureCompare(a, b string) bool {
    if len(a) != len(b) {
        return false
    }
    var result byte
    for i := 0; i < len(a); i++ {
        result |= a[i] ^ b[i]
    }
    return result == 0
}

// Write JSON response with proper headers
func writeJSON(w http.ResponseWriter, status int, resp modCheckResponse) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(resp)
}
