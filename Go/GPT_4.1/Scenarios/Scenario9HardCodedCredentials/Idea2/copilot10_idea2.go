package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "time"
)

// Simulated user store (for demonstration only; do not hardcode credentials in production)
var moderators = map[string]string{
    "moduser1": "StrongPassword123!",
    "moduser2": "Another$ecureP@ss",
}

// Input validation: allow only alphanumeric usernames, 3-32 chars
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

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

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Input validation and sanitization
    username = html.EscapeString(username)
    password = html.EscapeString(password)

    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username format", http.StatusBadRequest)
        return
    }
    if len(password) < 8 || len(password) > 64 {
        http.Error(w, "Invalid password length", http.StatusBadRequest)
        return
    }

    // Prevent timing attacks by always checking the map
    storedPassword, ok := moderators[username]
    isModerator := ok && subtleConstantTimeCompare(storedPassword, password)

    // Respond with JSON, never echo credentials
    w.Header().Set("Content-Type", "application/json")
    resp := modCheckResponse{
        IsModerator: isModerator,
        Message:     "Moderator status checked.",
    }
    json.NewEncoder(w).Encode(resp)
}

// Constant-time string comparison to prevent timing attacks
func subtleConstantTimeCompare(a, b string) bool {
    if len(a) != len(b) {
        return false
    }
    result := byte(0)
    for i := 0; i < len(a); i++ {
        result |= a[i] ^ b[i]
    }
    return result == 0
}
