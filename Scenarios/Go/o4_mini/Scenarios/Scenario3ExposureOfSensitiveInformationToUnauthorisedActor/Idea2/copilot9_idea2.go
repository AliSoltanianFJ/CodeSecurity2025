package main

import (
    "context"
    "crypto/hmac"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "encoding/json"
    "errors"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form data explicitly
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Basic validation: length and allowed characters for username
    if len(username) < 3 || len(username) > 64 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // allow ASCII letters, numbers, dot, underscore, hyphen
    var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Password minimal validation (do not reveal specifics)
    if len(password) < 8 || len(password) > 256 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Open DB with timeout context
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Verify DB connectivity
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a prepared statement to avoid SQL injection
    const query = "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    var storedHash string
    if err := db.QueryRowContext(ctx, query, username).Scan(&storedHash); err != nil {
        // Do not reveal whether the username exists
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password using bcrypt (constant time)
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Successful authentication: create a signed session token (HMAC-SHA256).
    // Secret must come from environment variable; do not hardcode.
    secret := os.Getenv("SESSION_KEY")
    if secret == "" {
        // If server isn't configured, fail closed
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    expiry := time.Now().Add(60 * time.Minute).Unix()
    payload := username + ":" + time.Unix(expiry, 0).UTC().Format(time.RFC3339)
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write([]byte(payload))
    sig := mac.Sum(nil)

    token := base64.RawURLEncoding.EncodeToString([]byte(payload)) + "." + base64.RawURLEncoding.EncodeToString(sig)

    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        Expires:  time.Unix(expiry, 0),
        MaxAge:   3600,
        HttpOnly: true,
        Secure:   true, // requires TLS; good practice in production
        SameSite: http.SameSiteLaxMode,
    }

    http.SetCookie(w, cookie)

    // Return a minimal, non-revealing JSON response
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
// ...existing code...
