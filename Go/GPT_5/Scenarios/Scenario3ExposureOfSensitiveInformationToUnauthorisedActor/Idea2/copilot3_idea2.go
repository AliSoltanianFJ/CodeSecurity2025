// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "net/http"
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

    // limit request body size to prevent large payload abuse
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // validate inputs (no echoing back user input)
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // allow a conservative username charset and reasonable length
    var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    var storedHash string
    // parameterised query to avoid SQL injection
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        // don't reveal whether username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // compare password using bcrypt
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // generate a secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(tokenBytes)

    // TODO: persist sessionToken server-side (DB or in-memory store) with an expiry.
    // For security do not expose internal errors or sensitive detail to the client.

    // set cookie securely (Secure flag only when using TLS)
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        MaxAge:   3600,
        Secure:   r.TLS != nil,
    }
    http.SetCookie(w, cookie)

    // minimal success response, no sensitive data returned
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("OK"))
}
// ...existing code...
