package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "net/http"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Protect against very large request bodies
    r.Body = http.MaxBytesReader(w, r.Body, 10*1024) // 10 KiB max for login payloads
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    // Retrieve and sanitise inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation to avoid abusive input sizes and control chars
    if username == "" || password == "" || len(username) > 128 || len(password) > 1024 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Open DB connection (ensure DSN is stored securely in real apps, not hard-coded)
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a short context timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Use parameterised query to prevent SQL injection
    var storedHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        // Do not reveal whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password using bcrypt (constant-time)
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a secure random session token (store server-side in real apps)
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(b)

    // Set a secure, HttpOnly cookie. Only set Secure flag when TLS is used.
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        HttpOnly: true,
        Secure:   r.TLS != nil,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    // Successful login - do not echo sensitive info back to the client
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("OK"))
}

// ...existing code...
