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

// Example: In production, use a secure database and hashed passwords!
var moderators = map[string]string{
    "moduser1": "StrongPassword123!",
    "moduser2": "Another$ecureP@ss",
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

    // Limit input size to prevent buffer overflow
    r.ParseForm()
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Input validation and sanitization
    username = strings.TrimSpace(username)
    password = strings.TrimSpace(password)
    if len(username) > 32 || len(password) > 64 {
        http.Error(w, "Input too long", http.StatusBadRequest)
        return
    }
    if username == "" || password == "" {
        http.Error(w, "Missing username or password", http.StatusBadRequest)
        return
    }

    // Prevent XSS by escaping output (if echoed)
    safeUsername := html.EscapeString(username)

    // Check credentials (in production, use hashed passwords and secure storage)
    isMod := false
    if storedPass, ok := moderators[username]; ok && storedPass == password {
        isMod = true
    }

    // Respond with JSON (never echo passwords)
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response{
        IsModerator: isMod,
        Message:     fmt.Sprintf("User %s moderator status checked.", safeUsername),
    })
}
