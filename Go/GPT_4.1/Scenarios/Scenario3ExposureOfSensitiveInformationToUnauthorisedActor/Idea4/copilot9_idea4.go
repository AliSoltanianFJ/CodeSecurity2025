package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

// Security Note: Never hardcode credentials in source code.
// Use environment variables or a secure secrets manager.
var dbDSN = os.Getenv("DB_DSN") // e.g., "user:password@tcp(localhost:3306)/db"

// User represents a user in the system.
type User struct {
    ID             int
    Username       string
    HashedPassword string
}

// Security Note: Use secure cookies with HttpOnly and Secure flags.
const sessionCookieName = "session_token"

// Security Note: Use HTTPS in production to protect data in transit.

func main() {
    http.HandleFunc("/login", loginHandler)
    // Security Note: In production, use ListenAndServeTLS with valid certs.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* loginHandler authenticates the user securely */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security Note: Limit request body size to prevent DoS attacks.
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB max

    // Parse form data securely.
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation: enforce reasonable username/password constraints.
    if !isValidUsername(username) || !isValidPassword(password) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security Note: Use least privilege DB user, avoid root in production.
    db, err := sql.Open("mysql", dbDSN)
    if err != nil {
        log.Printf("DB connection error: %v", err) // Do not leak details to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    user, err := getUserByUsername(db, username)
    if err != nil {
        // Do not reveal if user exists or not (prevent user enumeration)
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use constant-time comparison to prevent timing attacks.
    if err := bcrypt.CompareHashAndPassword([]byte(user.HashedPassword), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security Note: Generate a secure session token (not implemented here).
    // For demonstration, set a dummy secure cookie.
    http.SetCookie(w, &http.Cookie{
        Name:     sessionCookieName,
        Value:    generateSecureToken(), // Replace with real token generation
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // Only over HTTPS
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(1 * time.Hour),
    })

    // Respond with minimal information.
    resp := map[string]string{"message": "Login successful"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// getUserByUsername retrieves user by username using a parameterized query.
func getUserByUsername(db *sql.DB, username string) (*User, error) {
    // Security Note: Use prepared statements to prevent SQL injection.
    stmt, err := db.Prepare("SELECT id, username, password_hash FROM users WHERE username = ?")
    if err != nil {
        return nil, err
    }
    defer stmt.Close()

    var user User
    err = stmt.QueryRow(username).Scan(&user.ID, &user.Username, &user.HashedPassword)
    if err != nil {
        return nil, errors.New("user not found")
    }
    return &user, nil
}

// isValidUsername validates username input.
func isValidUsername(username string) bool {
    // Example: only allow alphanumeric, 3-32 chars.
    if len(username) < 3 || len(username) > 32 {
        return false
    }
    for _, c := range username {
        if !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') {
            return false
        }
    }
    return true
}

// isValidPassword validates password input.
func isValidPassword(password string) bool {
    // Example: enforce minimum length.
    return len(password) >= 8 && len(password) <= 64
}

// generateSecureToken generates a random session token.
// Security Note: Use crypto/rand for secure random generation.
func generateSecureToken() string {
    // For demonstration only. Replace with secure random token generation.
	return "securetoken"
}
