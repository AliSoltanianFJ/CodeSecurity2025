package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "time"
)

// Simulated moderator credentials (should be stored securely in production)
var moderators = map[string]string{
    "moduser1": "StrongPassword123!",
    "moduser2": "Another$ecureP@ssw0rd",
}

// Input validation: usernames must be alphanumeric, 3-20 chars
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_]{3,20}$`)

type modResponse struct {
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

    // Limit request body size (defense-in-depth)
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Sanitize input for logging (never echo passwords)
    safeUsername := html.EscapeString(username)

    // Validate input
    if !validUsername.MatchString(username) {
        writeJSON(w, http.StatusBadRequest, modResponse{
            IsModerator: false,
            Message:     "Invalid username format.",
        })
        return
    }
    if len(password) < 8 || len(password) > 64 {
        writeJSON(w, http.StatusBadRequest, modResponse{
            IsModerator: false,
            Message:     "Invalid password length.",
        })
        return
    }

    // Check credentials (constant time compare for security)
    storedPassword, ok := moderators[username]
    isModerator := ok && secureCompare(storedPassword, password)

    // Never reveal which part failed
    if isModerator {
        writeJSON(w, http.StatusOK, modResponse{
            IsModerator: true,
            Message:     fmt.Sprintf("User %s is a moderator.", safeUsername),
        })
    } else {
        writeJSON(w, http.StatusOK, modResponse{
            IsModerator: false,
            Message:     "User is not a moderator.",
        })
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

// Write JSON response with proper headers
func writeJSON(w http.ResponseWriter, status int, resp modResponse) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(resp)
}
