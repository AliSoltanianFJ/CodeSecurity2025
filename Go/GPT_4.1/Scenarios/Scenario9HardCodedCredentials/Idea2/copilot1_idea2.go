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
    "moduser1": "StrongPassword123!",
    "moduser2": "Another$ecureP@ssw0rd",
}

// Input validation: allow only alphanumeric usernames, 3-32 chars
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

// Response structure
type modResponse struct {
    IsModerator bool   `json:"isModerator"`
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

    // Limit request body size (defensive, though GET should have no body)
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Sanitize and validate input
    username = html.EscapeString(username)
    password = html.EscapeString(password)

    if !validUsername.MatchString(username) {
        writeJSON(w, modResponse{IsModerator: false, Message: "Invalid username format"})
        return
    }
    if len(password) < 8 || len(password) > 64 {
        writeJSON(w, modResponse{IsModerator: false, Message: "Invalid password length"})
        return
    }

    // Constant-time comparison to prevent timing attacks
    isMod := false
    if storedPass, ok := moderators[username]; ok {
        if subtleConstantTimeCompare(storedPass, password) {
            isMod = true
        }
    }

    // Do not reveal which field was incorrect
    if isMod {
        writeJSON(w, modResponse{IsModerator: true, Message: "User is a moderator"})
    } else {
        writeJSON(w, modResponse{IsModerator: false, Message: "Invalid credentials"})
    }
}

// Constant-time string comparison
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

// Write JSON response with proper headers
func writeJSON(w http.ResponseWriter, resp modResponse) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Prevent XSS in JSON responses
    w.Header().Set("X-Content-Type-Options", "nosniff")
    json.NewEncoder(w).Encode(resp)
}
