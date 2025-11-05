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

// In a real application, use a secure database and secrets management.
// For demonstration, load users from a secure file at startup.
var users = map[string]User{}

// loadUsers loads user data from a secure file (JSON format).
func loadUsers(filename string) error {
    file, err := os.Open(filename)
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

func main() {
    // Secure configuration: listen only on localhost for demo; use HTTPS in production.
    addr := "127.0.0.1:8080"

    // Load users from a secure file (do not hardcode credentials).
    if err := loadUsers("users.json"); err != nil {
        log.Fatalf("Failed to load users: %v", err)
    }

    http.HandleFunc("/check_mod", checkModHandler)
    // In production, use ListenAndServeTLS with valid certificates.
    log.Printf("Listening securely on %s", addr)
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("Server error: %v", err)
    }
}

/* checkMod securely checks if the provided credentials belong to a moderator.
   - All inputs are validated and sanitized.
   - Passwords are compared using constant-time comparison.
   - No sensitive information is leaked in errors.
   - No secrets are hardcoded.
*/
func checkMod(username, password string) (bool, error) {
    // Input validation: usernames must be 3-32 alphanumeric chars (no spaces)
    if len(username) < 3 || len(username) > 32 {
        return false, errors.New("invalid username length")
    }
    if !isAlphanumeric(username) {
        return false, errors.New("invalid username characters")
    }
    // Password length check (min 8, max 64 for demo)
    if len(password) < 8 || len(password) > 64 {
        return false, errors.New("invalid password length")
    }

    // Normalize username for lookup
    user, ok := users[strings.ToLower(username)]
    if !ok {
        // Do not reveal if user exists
        return false, errors.New("invalid credentials")
    }

    // Use bcrypt for password verification (constant-time)
    err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password))
    if err != nil {
        return false, errors.New("invalid credentials")
    }

    // Check role using constant-time comparison
    if subtle.ConstantTimeCompare([]byte(user.Role), []byte("moderator")) == 1 {
        return true, nil
    }
    return false, nil
}

// isAlphanumeric checks if a string contains only letters and digits.
func isAlphanumeric(s string) bool {
    for _, r := range s {
        if !(r >= 'a' && r <= 'z') &&
            !(r >= 'A' && r <= 'Z') &&
            !(r >= '0' && r <= '9') {
            return false
        }
    }
    return true
}

// checkModHandler handles the /check_mod endpoint securely.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce HTTPS in production (redirect if not secure)
    if r.TLS == nil {
        http.Error(w, "HTTPS required", http.StatusUpgradeRequired)
        return
    }

    // Only allow GET requests for this endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and sanitize query parameters
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password") // Do not trim password

    // Validate and check moderator status
    isMod, err := checkMod(username, password)
    if err != nil {
        // Generic error message to avoid leaking info
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Respond with JSON (safe output encoding)
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isMod})
}
