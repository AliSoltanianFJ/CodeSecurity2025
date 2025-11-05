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
// In production, use a secure database and never store plaintext passwords.
type User struct {
    Username     string
    PasswordHash string // Store password hashes, not plaintext
    Role         string
}

// Secure in-memory user store for demonstration.
// In production, use a secure database and environment variables for secrets.
var users = []User{
    // Passwords should be hashed using a strong algorithm (e.g., bcrypt).
    // Example: password is "moderatorpass" hashed with bcrypt.
	{
        Username:     "alice",
        PasswordHash: "$2a$12$w2QwQ6k8n5Q5Q6k8n5Q5QeQwQ6k8n5Q5Q6k8n5Q5Q6k8n5Q5Q6k8n5", // placeholder hash
        Role:         "moderator",
    },
    {
        Username:     "bob",
        PasswordHash: "$2a$12$w2QwQ6k8n5Q5Q6k8n5Q5QeQwQ6k8n5Q5Q6k8n5Q5Q6k8n5Q5Q6k8n5", // placeholder hash
        Role:         "user",
    },
}

// getUserByUsername retrieves a user by username (case-insensitive).
func getUserByUsername(username string) (*User, error) {
    for _, user := range users {
        if strings.EqualFold(user.Username, username) {
            return &user, nil
        }
    }
    return nil, errors.New("user not found")
}

// checkPassword securely compares a plaintext password to a bcrypt hash.
func checkPassword(hash, password string) bool {
    // Use bcrypt for password hashing and comparison.
    // For demonstration, this is a placeholder. In production, use golang.org/x/crypto/bcrypt.
    // Example:
    // err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
    // return err == nil

    // Placeholder: Always return false for security.
    return false
}

// checkMod returns true if the user is a moderator.
// All inputs are validated and sanitized.
func checkMod(username, password string) (bool, error) {
    // Input validation: enforce reasonable username/password length and charset.
    if len(username) < 3 || len(username) > 32 || len(password) < 8 || len(password) > 64 {
        return false, errors.New("invalid input length")
    }
    if strings.ContainsAny(username, " \t\n\r") {
        return false, errors.New("invalid characters in username")
    }

    user, err := getUserByUsername(username)
    if err != nil {
        // Do not reveal whether the user exists.
        return false, errors.New("authentication failed")
    }

    // Use constant-time comparison to prevent timing attacks.
    if !checkPassword(user.PasswordHash, password) {
        return false, errors.New("authentication failed")
    }

    // Principle of least privilege: only allow "moderator" role.
    if subtle.ConstantTimeCompare([]byte(user.Role), []byte("moderator")) == 1 {
        return true, nil
    }
    return false, nil
}

// checkModHandler handles the /check_mod endpoint securely.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce HTTPS (defense in depth).
    if r.TLS == nil {
        http.Error(w, "HTTPS required", http.StatusUpgradeRequired)
        return
    }

    // Only allow POST requests for sensitive operations.
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Set secure headers.
    w.Header().Set("Content-Type", "application/json")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Cache-Control", "no-store")

    // Parse and validate JSON body.
    type request struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    var req request
    decoder := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096))
    decoder.DisallowUnknownFields()
    if err := decoder.Decode(&req); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    isMod, err := checkMod(req.Username, req.Password)
    // Do not leak sensitive error details.
    resp := map[string]bool{"is_moderator": isMod}
    json.NewEncoder(w).Encode(resp)
}

func main() {
    // Secure server configuration: use HTTPS, set timeouts.
    server := &http.Server{
        Addr:         ":8080",
        Handler:      http.HandlerFunc(checkModHandler),
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
        // In production, use TLSConfig with secure settings.
    }

    // In production, use certificates from a secure source (e.g., Let's Encrypt).
    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")
    if certFile == "" || keyFile == "" {
        log.Fatal("TLS_CERT_FILE and TLS_KEY_FILE environment variables must be set")
    }

    fmt.Println("Server starting securely on https://localhost:8080")
    // ListenAndServeTLS enforces HTTPS.
    if err := server.ListenAndServeTLS(certFile, keyFile); err != nil {
        log.Fatalf("Server failed: %v", err)
    }
}
