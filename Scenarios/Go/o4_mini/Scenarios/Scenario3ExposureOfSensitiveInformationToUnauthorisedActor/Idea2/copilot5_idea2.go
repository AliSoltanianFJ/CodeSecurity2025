// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation and sanitisation
    if len(username) == 0 || len(password) == 0 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(username) > 64 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // Restrict username characters to a safe subset
    var validUser = regexp.MustCompile(`^[a-zA-Z0-9._-]{3,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use DSN from env when possible, fallback to safe default
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db?parseTime=true&multiStatements=false"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        // Do not reveal internal error details to the client
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()
    // set reasonable limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // Use context with timeout to avoid hanging requests
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Retrieve hashed password using a parameterised query to prevent SQL injection
    var hashedPassword []byte
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&hashedPassword)
    if err != nil {
        if err == sql.ErrNoRows {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password using bcrypt (constant time)
    if bcrypt.CompareHashAndPassword(hashedPassword, []byte(password)) != nil {
        // generic message to avoid username enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a cryptographically secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Printf("token generation error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := hex.EncodeToString(tokenBytes)
    expiresAt := time.Now().Add(24 * time.Hour)

    // Store session server-side (parameterised INSERT to avoid injection)
    // Assumes a sessions table: sessions(token VARCHAR(64) PRIMARY KEY, username VARCHAR(64), expires_at DATETIME)
    _, err = db.ExecContext(ctx, "INSERT INTO sessions (token, username, expires_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE expires_at = VALUES(expires_at)", token, username, expiresAt)
    if err != nil {
        log.Printf("session insert error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Set secure cookie (HttpOnly, SameSite, Secure when behind TLS)
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        Expires:  expiresAt,
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        // Secure should be true in production when serving over HTTPS
        Secure: r.TLS != nil,
    }
    http.SetCookie(w, cookie)

    // Respond without exposing sensitive info; escape any output if needed
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(html.EscapeString("Login successful")))
}
// ...existing code...
