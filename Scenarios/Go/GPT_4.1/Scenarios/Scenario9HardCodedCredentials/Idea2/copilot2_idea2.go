package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "time"
)

// Simulated user store (in production, use a secure database and hashed passwords)
var moderators = map[string]string{
    "moduser1": "StrongPassword123!",
	"moduser2": "Another$ecureP@ssw0rd",
}

// Input validation: allow only alphanumeric usernames, 3-32 chars
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

type modCheckResponse struct {
    IsModerator bool   `json:"is_moderator"`
    Message     string `json:"message"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // Use a server with timeouts to prevent Slowloris attacks
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

    // Limit request body size (not strictly needed for GET, but good practice)
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Sanitize and validate input
    username = html.EscapeString(username)
    password = html.EscapeString(password)

    if !usernameRegex.MatchString(username) {
        writeJSONResponse(w, false, "Invalid username format")
        return
    }
    if len(password) < 8 || len(password) > 64 {
        writeJSONResponse(w, false, "Invalid password length")
        return
    }

    // Constant-time comparison to prevent timing attacks
    if storedPass, ok := moderators[username]; ok && secureCompare(storedPass, password) {
        writeJSONResponse(w, true, "User is a moderator")
        return
    }

    writeJSONResponse(w, false, "User is not a moderator")
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
func writeJSONResponse(w http.ResponseWriter, isMod bool, msg string) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Prevent XSS in message
    resp := modCheckResponse{
        IsModerator: isMod,
        Message:     html.EscapeString(msg),
    }
    json.NewEncoder(w).Encode(resp)
}
