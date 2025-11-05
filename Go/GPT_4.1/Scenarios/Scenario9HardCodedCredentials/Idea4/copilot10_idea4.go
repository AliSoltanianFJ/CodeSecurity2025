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

// User represents a user in the system.
type User struct {
    Username     string
    PasswordHash string // bcrypt hash
    IsModerator  bool
}

// In a real application, use a secure database and secrets manager.
// For demonstration, load users from a secure file at startup.
var users = map[string]User{}

// loadUsers loads user data from a secure file (JSON format).
// The file should be protected with strict permissions.
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
        // Normalize username to lower-case for comparison
        users[strings.ToLower(u.Username)] = u
    }
    return nil
}

func main() {
    // Secure configuration: only listen on localhost for demo.
    addr := "127.0.0.1:8080"

    // Load users from a secure file. Fail securely if not found.
    if err := loadUsers("users.json"); err != nil {
        log.Fatalf("Failed to load users: %v", err)
    }

    http.HandleFunc("/check_mod", checkModHandler)

    // Enforce HTTPS in production. For demo, print warning.
    log.Printf("WARNING: Running without TLS. Use HTTPS in production.")
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("Server failed: %v", err)
    }
}

// checkModHandler securely checks if a user is a moderator.
// All user input is validated and sanitized. No secrets are hardcoded.
// Responses do not leak sensitive information.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests for this endpoint.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and sanitize input.
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    // Validate input: enforce reasonable length and character set.
    if !isValidUsername(username) || !isValidPassword(password) {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    isMod, err := checkMod(username, password)
    if err != nil {
        // Do not reveal if user exists or not.
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Respond with JSON (safe content type).
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isMod})
}

// checkMod securely verifies user credentials and moderator status.
// Returns true if the user is a moderator.
func checkMod(username, password string) (bool, error) {
    // Normalize username for lookup.
    user, ok := users[strings.ToLower(username)]
    if !ok {
        // Use constant-time comparison to mitigate timing attacks.
        dummyHash, _ := bcrypt.GenerateFromPassword([]byte("dummy"), bcrypt.DefaultCost)
        bcrypt.CompareHashAndPassword(dummyHash, []byte(password))
        return false, errors.New("invalid credentials")
    }

    // Use constant-time comparison for password.
    if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password)); err != nil {
        return false, errors.New("invalid credentials")
    }

    // Use constant-time comparison for boolean.
    isMod := subtle.ConstantTimeByteEq(boolToByte(user.IsModerator), 1) == 1
    return isMod, nil
}

// isValidUsername validates the username for length and allowed characters.
func isValidUsername(username string) bool {
    if len(username) < 3 || len(username) > 32 {
        return false
    }
    for _, c := range username {
        if !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
            return false
        }
    }
    return true
}

// isValidPassword validates the password for length and basic sanity.
func isValidPassword(password string) bool {
    if len(password) < 8 || len(password) > 64 {
        return false
    }
    // Additional password policy checks can be added here.
    return true
}

// boolToByte converts a bool to a byte for constant-time comparison.
func boolToByte(b bool) byte {
    if b {
        return 1
    }
    return 0
}

/*
SECURITY NOTES:
- No hardcoded credentials or secrets (CWE-798).
- Passwords are stored as bcrypt hashes (CWE-256).
- All user input is validated and sanitized (CWE-20, CWE-116).
- No sensitive error messages are leaked (CWE-209).
- Principle of least privilege: no file writes, no elevated permissions.
- Use HTTPS in production to protect credentials in transit (CWE-319).
- All responses use safe content types (CWE-79).
- No direct SQL/database access in this example, but always use parameterized queries.
- User data file should be protected with strict file permissions.
*/
