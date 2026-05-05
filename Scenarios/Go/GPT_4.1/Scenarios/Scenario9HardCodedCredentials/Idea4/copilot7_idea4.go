package main

import (
    "encoding/json"
    "errors"
    "fmt"
    "log"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

// User represents a user record with secure password storage and role.
type User struct {
    Username     string
    PasswordHash string // bcrypt hash
    Role         string // e.g., "moderator", "user"
}

// In a real application, use a secure database and secrets management.
// For demonstration, load users from a JSON file with bcrypt-hashed passwords.
var users = map[string]User{}

// loadUsers loads user data from a secure source (here, a JSON file).
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
        // Usernames are stored in lowercase for case-insensitive comparison.
        users[strings.ToLower(u.Username)] = u
    }
    return nil
}

// authenticateUser securely checks credentials and returns the user if valid.
func authenticateUser(username, password string) (*User, error) {
    // Enforce input length limits to prevent DoS and buffer overflows.
    if len(username) < 3 || len(username) > 64 || len(password) < 8 || len(password) > 128 {
        return nil, errors.New("invalid credentials")
    }
    // Sanitize username: allow only alphanumeric and underscore.
    for _, r := range username {
        if !(r >= 'a' && r <= 'z') && !(r >= 'A' && r <= 'Z') && !(r >= '0' && r <= '9') && r != '_' {
            return nil, errors.New("invalid credentials")
        }
    }
    user, ok := users[strings.ToLower(username)]
    if !ok {
        return nil, errors.New("invalid credentials")
    }
    // Use constant-time comparison for password hashes.
    if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password)); err != nil {
        return nil, errors.New("invalid credentials")
    }
    return &user, nil
}

func main() {
    // Securely load users from a file (do not hardcode credentials).
    if err := loadUsers("users.json"); err != nil {
        log.Fatalf("Failed to load users: %v", err)
    }
    // Always use HTTPS in production. This is for demonstration only.
    http.HandleFunc("/check_mod", checkModHandler)
    // Listen on localhost only for least privilege.
    log.Fatal(http.ListenAndServe("127.0.0.1:8080", nil))
}

/* checkModHandler securely checks if the user is a moderator.
   All input is validated and sanitized. No sensitive info is leaked. */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce HTTPS (redirect if not secure).
    if r.TLS == nil {
        http.Error(w, "HTTPS required", http.StatusUpgradeRequired)
        return
    }

    // Use POST only for credentials to avoid leaking in URLs.
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form data securely.
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    user, err := authenticateUser(username, password)
    if err != nil {
        // Do not reveal if username or password is incorrect.
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }

    // Principle of least privilege: only allow if role is exactly "moderator".
    isModerator := user.Role == "moderator"

    // Respond with JSON (safe content type).
    w.Header().Set("Content-Type", "application/json")
    // Do not leak any sensitive info in the response.
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isModerator})
}
