package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "fmt"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and validate input
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation and sanitisation
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    if len(username) > 64 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    // Allow only a safe username character set
    var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Open DB with context and timeout
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true")
    if err != nil {
        // Do not return internal details to the client
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a prepared query to avoid injection and fetch stored password hash
    var passwordHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        // Generic message to avoid username enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare the provided password with the stored bcrypt hash
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a cryptographically secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Printf("session token generation error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := base64.RawURLEncoding.EncodeToString(tokenBytes)

    // In a real application, store the sessionToken server-side (DB or secure store) associated with the user and expiry.
    // Here we set a secure cookie (HttpOnly + SameSite). Secure flag is recommended in production (requires HTTPS).
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set to true in production when using TLS
        SameSite: http.SameSiteLaxMode,
        MaxAge:   3600,
    }
    http.SetCookie(w, cookie)

    // Return a minimal, non-sensitive success response
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    fmt.Fprint(w, "Login successful")
}

// ...existing code...
