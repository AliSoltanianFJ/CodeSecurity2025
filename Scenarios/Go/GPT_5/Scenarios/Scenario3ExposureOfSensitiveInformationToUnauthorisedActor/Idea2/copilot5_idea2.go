package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "html"
    "log"
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

    // Parse and normalise input
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation: present and reasonable length
    if len(username) < 3 || len(username) > 128 || len(password) < 8 || len(password) > 256 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Restrict username characters to a safe subset
    if ok, _ := regexp.MatchString(`^[A-Za-z0-9._@-]+$`, username); !ok {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Open DB with short-lived context and close promptly
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        log.Printf("db open error: %v", err) // internal logging only
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a prepared/query context to avoid SQL injection
    var storedHash string
    query := "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    err = db.QueryRowContext(ctx, query, username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        // Do not reveal whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password using bcrypt
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // Authentication failed; generic message
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authenticated: create a cryptographically secure session token
    var b [32]byte
    if _, err := rand.Read(b[:]); err != nil {
        log.Printf("token gen error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := base64.RawURLEncoding.EncodeToString(b[:])

    // NOTE: In a real app you must persist the session server-side (DB, cache) with expiration.
    // Here we set a secure cookie; keep cookie content opaque and HttpOnly to prevent XSS access.
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // requires HTTPS in production
        SameSite: http.SameSiteStrictMode,
        MaxAge:   3600, // 1 hour
    }
    http.SetCookie(w, cookie)

    // Respond without echoing sensitive input; escape any text if needed.
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(html.EscapeString("Login successful")))
}

// ...existing code...
