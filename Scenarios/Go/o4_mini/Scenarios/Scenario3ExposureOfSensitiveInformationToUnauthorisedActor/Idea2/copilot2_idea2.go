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
    // Limit request size to mitigate DoS/large payloads
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form values (FormValue will call ParseForm internally, but ensure context)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic input validation / sanitisation
    // Username: allow letters, numbers, underscore, dash; length 3..32
    if len(username) < 3 || len(username) > 32 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if matched, _ := regexp.MatchString(`^[A-Za-z0-9_-]+$`, username); !matched {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Password policy check: min 8, max 128 (do not echo password back)
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Open DB with short context timeout
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        // Do not reveal internal details
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Verify DB is reachable
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a parameterised query to avoid SQL injection
    var storedHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&storedHash)
    if err != nil {
        // Treat "no rows" the same as wrong credentials to avoid user enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Compare bcrypt hash in constant time
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // Wrong password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authentication successful: create a secure random session token.
    // NOTE: For a production system, you MUST store this token server-side (e.g. sessions table, cache)
    // and validate it on subsequent requests. This example only demonstrates secure token generation and cookie flags.
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(tokenBytes)

    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        HttpOnly: true,
        Secure:   r.TLS != nil, // set Secure only when request is over TLS
        SameSite: http.SameSiteLaxMode,
        Path:     "/",
        // Consider setting an appropriate MaxAge or Expires and storing server-side session state
    }
    http.SetCookie(w, cookie)

    // Generic success message (do not reveal private information or echo input)
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}

// ...existing code...
