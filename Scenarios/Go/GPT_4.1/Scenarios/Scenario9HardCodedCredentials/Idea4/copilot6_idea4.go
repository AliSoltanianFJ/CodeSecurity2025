package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "fmt"
    "log"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

// User represents a user record with a securely hashed password and role.
type User struct {
    Username     string
    PasswordHash string // bcrypt hash
    Role         string // e.g., "moderator", "user"
}

// In a real application, use a secure database and secrets manager.
// For demonstration, load users from a JSON file with bcrypt-hashed passwords.
var users = map[string]User{}

// loadUsers loads user data from a secure file (not hardcoded).
func loadUsers(path string) error {
    file, err := os.Open(path)
    if err != nil {
        return err
    }
    defer file.Close()
    decoder := json.NewDecoder(file)
    var loadedUsers []User
    if err := decoder.Decode(&loadedUsers); err != nil {
        return err
    }
    for _, u := range loadedUsers {
        // Normalize username to lowercase for consistent comparison
        users[strings.ToLower(u.Username)] = u
    }
    return nil
}

// checkMod securely checks if the provided credentials belong to a moderator.
// Returns true if the user is a moderator, false otherwise.
func checkMod(username, password string) (bool, error) {
    // Input validation: enforce reasonable username/password length and charset
    if len(username) < 3 || len(username) > 64 || len(password) < 8 || len(password) > 128 {
        return false, errors.New("invalid input length")
    }
    if strings.ContainsAny(username, " \t\n\r") {
        return false, errors.New("invalid characters in username")
    }
    // Normalize username
    normalized := strings.ToLower(username)
    user, exists := users[normalized]
    if !exists {
        // Do not reveal if user exists
        return false, nil
    }
    // Use constant-time comparison to prevent timing attacks
    err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password))
    if err != nil {
        return false, nil
    }
    // Check role using constant-time comparison
    if subtle.ConstantTimeCompare([]byte(user.Role), []byte("moderator")) == 1 {
        return true, nil
    }
    return false, nil
}

func main() {
    // Secure configuration: only listen on localhost for demo; use HTTPS in production.
    addr := "127.0.0.1:8080"
    // Load users from a secure file (do not hardcode credentials)
    if err := loadUsers("users.json"); err != nil {
        log.Fatalf("Failed to load users: %v", err)
    }
    http.HandleFunc("/check_mod", checkModHandler)
    log.Printf("Listening securely on http://%s", addr)
    // In production, use ListenAndServeTLS with valid certificates.
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("Server error: %v", err)
    }
}

// checkModHandler handles moderator check requests securely.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce HTTPS in production
    if r.TLS == nil {
        http.Error(w, "HTTPS required", http.StatusUpgradeRequired)
        return
    }
    // Only allow GET or POST
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Parse and sanitize input
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")
    // Avoid logging sensitive data
    ok, err := checkMod(username, password)
    if err != nil {
        // Generic error message to avoid information leakage
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // Respond with JSON
    w.Header().Set("Content-Type", "application/json")
    resp := map[string]bool{"is_moderator": ok}
    json.NewEncoder(w).Encode(resp)
}
