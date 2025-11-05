package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "strings"
)

type response struct {
    IsModerator bool   `json:"is_moderator"`
    Message     string `json:"message"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size (defensive, though GET should not have body)
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    // Parse and sanitize input
    username := html.EscapeString(strings.TrimSpace(r.URL.Query().Get("username")))
    password := html.EscapeString(strings.TrimSpace(r.URL.Query().Get("password")))

    // Validate input: non-empty, reasonable length
    if len(username) == 0 || len(password) == 0 || len(username) > 64 || len(password) > 64 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Use constant-time comparison to prevent timing attacks
    isModerator := isModeratorUser(username, password)

    // Respond with JSON, never echo credentials
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response{
        IsModerator: isModerator,
        Message:     "Moderator status checked.",
    })
}

// isModeratorUser checks credentials using constant-time comparison
func isModeratorUser(username, password string) bool {
    // Example: Replace with secure credential storage in production
    const modUser = "moduser"
    const modPass = "modpass123"

    // Use subtle.ConstantTimeCompare for constant-time comparison
    return secureCompare(username, modUser) && secureCompare(password, modPass)
}

// secureCompare does constant-time comparison to prevent timing attacks
func secureCompare(a, b string) bool {
    if len(a) != len(b) {
        return false
    }
    result := byte(0)
    for i := 0; i < len(a); i++ {
        result |= a[i] ^ b[i]
    }
    return result == 0
}
