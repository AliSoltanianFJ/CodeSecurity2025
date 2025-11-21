// ...existing code...
package main

import (
    "crypto/subtle"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Println("starting server on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Always respond JSON and avoid reflecting any user input back to the client.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Accept only GET for this endpoint (reduce attack surface).
    if r.Method != http.MethodGet {
        http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
        return
    }

    // Parse and sanitize inputs
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    // Basic validation to prevent abusive input sizes and invalid characters.
    // Username: 1-64 chars, only letters, digits, underscore and hyphen.
    var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
    if username == "" || password == "" || !usernameRe.MatchString(username) {
        _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
        return
    }

    // Password length limits to avoid excessive resource usage.
    if len(password) < 8 || len(password) > 512 {
        _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
        return
    }

    // Load expected moderator credentials from environment variables.
    // Do NOT hard-code credentials in source.
    expectedUser := os.Getenv("MOD_USERNAME")
    expectedHash := os.Getenv("MOD_PASSWORD_HASH") // bcrypt hash

    // If configuration is missing, deny access without revealing details.
    if expectedUser == "" || expectedHash == "" {
        _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
        return
    }

    // Always perform the password hash comparison to help mitigate timing user enumeration.
    hashErr := bcrypt.CompareHashAndPassword([]byte(expectedHash), []byte(password))

    // Use constant-time username comparison.
    userMatch := subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) == 1

    isMod := userMatch && hashErr == nil

    // Do not include any sensitive details in the response.
    _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": isMod})
}
