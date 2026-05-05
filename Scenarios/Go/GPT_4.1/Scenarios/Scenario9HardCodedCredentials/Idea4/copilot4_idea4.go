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

    "golang.org/x/crypto/bcrypt"
)

// User represents a user record with a hashed password and role.
type User struct {
    Username     string
    PasswordHash string // bcrypt hash
    Role         string // e.g., "moderator", "user"
}

// In a real application, use a secure database and secrets management.
// For demonstration, load users from a JSON file with hashed passwords.
var users = map[string]User{}

// loadUsers loads user data from a secure location (e.g., environment variable or file).
func loadUsers() error {
    // Securely load user data from a file (not hardcoded).
    // The file path should be set via environment variable for flexibility and security.
    userFile := os.Getenv("USER_DATA_FILE")
    if userFile == "" {
        return errors.New("user data file not specified")
    }
    data, err := os.ReadFile(userFile)
    if err != nil {
        return fmt.Errorf("failed to read user data file: %w", err)
    }
    var loadedUsers []User
    if err := json.Unmarshal(data, &loadedUsers); err != nil {
        return fmt.Errorf("failed to parse user data: %w", err)
    }
    for _, u := range loadedUsers {
        users[strings.ToLower(u.Username)] = u
    }
    return nil
}

func main() {
    // Ensure user data is loaded securely at startup.
    if err := loadUsers(); err != nil {
        log.Fatalf("Startup error: %v", err)
    }

    // Enforce HTTPS by redirecting HTTP requests.
    go func() {
        // Listen on HTTP and redirect to HTTPS.
        http.ListenAndServe(":8080", http.HandlerFunc(redirectToHTTPS))
    }()

    // Use secure TLS configuration in production.
    server := &http.Server{
        Addr:              ":8443",
        Handler:           http.HandlerFunc(checkModHandler),
        ReadTimeout:       5 * time.Second,
        WriteTimeout:      10 * time.Second,
        IdleTimeout:       120 * time.Second,
        ReadHeaderTimeout: 2 * time.Second,
        // TLSConfig:        // Set strong TLS config here.
    }
    log.Println("Listening securely on :8443")
    // In production, use certificates from a secure source (e.g., Let's Encrypt).
    log.Fatal(server.ListenAndServeTLS("server.crt", "server.key"))
}

// redirectToHTTPS redirects all HTTP requests to HTTPS.
func redirectToHTTPS(w http.ResponseWriter, r *http.Request) {
    target := "https://" + r.Host + r.URL.RequestURI()
    http.Redirect(w, r, target, http.StatusMovedPermanently)
}

/* checkModHandler securely checks if the user is a moderator.
   - All inputs are validated and sanitized.
   - Passwords are compared using constant-time comparison.
   - No secrets are hardcoded.
   - Errors do not leak sensitive information.
*/
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests.
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce HTTPS.
    if r.TLS == nil {
        http.Error(w, "HTTPS required", http.StatusUpgradeRequired)
        return
    }

    // Parse and sanitize inputs.
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    // Validate input lengths to prevent resource exhaustion.
    if len(username) < 3 || len(username) > 64 || len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Prevent username enumeration by using constant-time checks.
    user, ok := users[strings.ToLower(username)]
    // Use a dummy hash to mitigate timing attacks.
    dummyHash, _ := bcrypt.GenerateFromPassword([]byte("dummyPassword123!"), bcrypt.DefaultCost)
    hashToCompare := dummyHash
    if ok {
        hashToCompare = []byte(user.PasswordHash)
    }
    // Use bcrypt for password comparison (constant-time).
    err := bcrypt.CompareHashAndPassword(hashToCompare, []byte(password))
    authenticated := (err == nil) && ok

    // Only return true if authenticated and user is a moderator.
    isModerator := false
    if authenticated && subtle.ConstantTimeCompare([]byte(user.Role), []byte("moderator")) == 1 {
        isModerator = true
    }

    // Respond with JSON (safe output encoding).
    w.Header().Set("Content-Type", "application/json")
    // Do not leak authentication failure reasons.
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isModerator})
}
