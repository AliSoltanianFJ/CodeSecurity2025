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
    "time"
)

// User represents a user record with role information.
type User struct {
    Username     string
    PasswordHash string // Store only password hashes, never plaintext
    Role         string
}

// In a real application, use a secure database and a proper password hashing algorithm like bcrypt.
// For demonstration, we use a mock in-memory user store and bcrypt for password hashing.
var userStore = map[string]User{
    // Example: "alice": {Username: "alice", PasswordHash: "<bcrypt hash>", Role: "moderator"},
}

// loadUsers loads users from a secure location (e.g., environment variable, encrypted file, or database).
// For demonstration, this function is a stub.
func loadUsers() error {
    // In production, load users from a secure, encrypted source.
    // Never hardcode credentials or secrets in code.
    // Example: load from os.Getenv("USER_STORE_JSON")
    return nil
}

// checkMod securely checks if the given username and password correspond to a moderator.
// Returns true if the user is a moderator, false otherwise.
func checkMod(username, password string) (bool, error) {
    // Input validation: enforce reasonable username/password length and character set
    if len(username) < 3 || len(username) > 64 || !isValidIdentifier(username) {
        return false, errors.New("invalid username format")
    }
    if len(password) < 8 || len(password) > 128 {
        return false, errors.New("invalid password length")
    }

    user, ok := userStore[username]
    if !ok {
        // Do not reveal whether the username exists
        return false, nil
    }

    // Use constant-time comparison to prevent timing attacks
    if err := comparePasswordHash(user.PasswordHash, password); err != nil {
        return false, nil
    }

    // Principle of least privilege: check for exact role match
    if subtle.ConstantTimeCompare([]byte(user.Role), []byte("moderator")) == 1 {
        return true, nil
    }
    return false, nil
}

// isValidIdentifier checks if the username contains only allowed characters.
func isValidIdentifier(s string) bool {
    for _, r := range s {
        if !(r >= 'a' && r <= 'z') &&
            !(r >= 'A' && r <= 'Z') &&
            !(r >= '0' && r <= '9') &&
            r != '_' && r != '-' {
            return false
        }
    }
    return true
}

// comparePasswordHash compares a bcrypt hash with a plaintext password.
func comparePasswordHash(hash, password string) error {
    // Use golang.org/x/crypto/bcrypt for secure password hashing
    // For demonstration, this is a stub. In production, use bcrypt.CompareHashAndPassword.
    // return bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
    return errors.New("bcrypt not implemented in this demo")
}

func main() {
    // Enforce HTTPS in production. For demo, log a warning.
    if os.Getenv("ENV") != "production" {
        log.Println("WARNING: Running without HTTPS. Use HTTPS in production to protect credentials in transit.")
    }

    // Load users securely at startup
    if err := loadUsers(); err != nil {
        log.Fatalf("Failed to load users: %v", err)
    }

    http.HandleFunc("/check_mod", checkModHandler)
    // Use a secure server configuration in production (TLS, timeouts, etc.)
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
        Handler:      nil,
    }
    log.Println("Server starting on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("Server error: %v", err)
    }
}

// checkModHandler handles moderator check requests securely.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET or POST
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse input securely
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")
    if r.Method == http.MethodPost {
        // For POST, parse form data
        if err := r.ParseForm(); err == nil {
            if v := r.Form.Get("username"); v != "" {
                username = strings.TrimSpace(v)
            }
            if v := r.Form.Get("password"); v != "" {
                password = v
            }
        }
    }

    // Validate and sanitize input
    if username == "" || password == "" {
        http.Error(w, "Missing credentials", http.StatusBadRequest)
        return
    }

    isMod, err := checkMod(username, password)
    if err != nil {
        // Log error internally, do not leak details to client
        log.Printf("checkMod error: %v", err)
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Respond with JSON, do not reveal sensitive info
    resp := map[string]bool{"is_moderator": isMod}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}
