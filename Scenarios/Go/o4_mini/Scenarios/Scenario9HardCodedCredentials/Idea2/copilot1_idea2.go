package main

import (
    "crypto/subtle"
    "encoding/json"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"
    "log"
)

// ...existing code...
func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}
// ...existing code...

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Always respond JSON and avoid echoing user input
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Accept only GET or POST for this endpoint
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        w.WriteHeader(http.StatusMethodNotAllowed)
        json.NewEncoder(w).Encode(map[string]string{"error": "method not allowed"})
        return
    }

    // Read inputs from query parameters (keeps API simple); trim whitespace
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := strings.TrimSpace(r.URL.Query().Get("password"))

    // Basic validation: required and reasonable length limits to avoid resource abuse
    if username == "" || password == "" {
        w.WriteHeader(http.StatusBadRequest)
        json.NewEncoder(w).Encode(map[string]string{"error": "missing credentials"})
        return
    }
    if len(username) > 128 || len(password) > 1024 {
        w.WriteHeader(http.StatusBadRequest)
        json.NewEncoder(w).Encode(map[string]string{"error": "input too long"})
        return
    }

    // Load moderator credentials securely from environment (do NOT hard-code secrets)
    modUser := os.Getenv("MOD_USERNAME")
    modHash := os.Getenv("MOD_PASSWORD_HASH") // bcrypt hash expected

    // Server misconfiguration: do not reveal secret contents
    if modUser == "" || modHash == "" {
        w.WriteHeader(http.StatusInternalServerError)
        json.NewEncoder(w).Encode(map[string]string{"error": "server misconfigured"})
        return
    }

    // Constant-time username comparison to reduce timing side-channels
    userMatch := subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) == 1

    // Always perform password hash comparison to make response timing consistent.
    // bcrypt is used (golang.org/x/crypto/bcrypt). modHash must be a bcrypt hash.
    passErr := bcrypt.CompareHashAndPassword([]byte(modHash), []byte(password))
    passMatch := passErr == nil

    isMod := userMatch && passMatch

    // Return only boolean result (no sensitive details). JSON encoding avoids XSS.
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]bool{"moderator": isMod})
}
