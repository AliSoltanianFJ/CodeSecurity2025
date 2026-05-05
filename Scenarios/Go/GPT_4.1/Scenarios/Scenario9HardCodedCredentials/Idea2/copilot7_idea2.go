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

type modResponse struct {
    IsModerator bool   `json:"is_moderator"`
    Message     string `json:"message"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // Use a server with timeouts for better security
    server := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    server.ListenAndServe()
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size (defense in depth)
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Input validation and sanitization
    username = html.EscapeString(username)
    password = html.EscapeString(password)

    if !validUsername.MatchString(username) {
        writeJSON(w, modResponse{IsModerator: false, Message: "Invalid username format"})
        return
    }

    // Constant-time comparison to prevent timing attacks
    if storedPass, ok := moderators[username]; ok && secureCompare(storedPass, password) {
        writeJSON(w, modResponse{IsModerator: true, Message: "User is a moderator"})
        return
    }

    writeJSON(w, modResponse{IsModerator: false, Message: "User is not a moderator"})
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
func writeJSON(w http.ResponseWriter, resp modResponse) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Prevent XSS in JSON responses
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    enc.Encode(resp)
}
