package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "time"
)

// Simulated user database (in production, use a secure database and hashed passwords)
var moderators = map[string]string{
    "moduser1": "StrongPassword123!",
	"moduser2": "Another$ecureP@ssw0rd",
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

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Sanitize and validate input
    username = html.EscapeString(username)
    password = html.EscapeString(password)

    if !validUsername.MatchString(username) {
        writeJSONResponse(w, false, "Invalid username format")
        return
    }

    // Prevent timing attacks by always checking password
    storedPassword, ok := moderators[username]
    isModerator := ok && secureCompare(storedPassword, password)

    if isModerator {
        writeJSONResponse(w, true, "User is a moderator")
    } else {
        writeJSONResponse(w, false, "User is not a moderator")
    }
}

// Constant time string comparison to prevent timing attacks
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

func writeJSONResponse(w http.ResponseWriter, isModerator bool, message string) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Prevent XSS in JSON responses
    resp := modCheckResponse{
        IsModerator: isModerator,
        Message:     html.EscapeString(message),
    }
    json.NewEncoder(w).Encode(resp)
}
