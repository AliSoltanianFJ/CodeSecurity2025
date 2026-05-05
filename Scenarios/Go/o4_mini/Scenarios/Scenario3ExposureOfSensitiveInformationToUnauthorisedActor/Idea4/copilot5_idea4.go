package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// Simple in-memory session store for demo only.
// In production, use a persistent store (Redis, DB) with proper eviction and rotation.
type sessionInfo struct {
    username string
    expires  time.Time
}

var (
    db       *sql.DB
    sessions = struct {
        sync.Mutex
        m map[string]sessionInfo
    }{m: make(map[string]sessionInfo)}
    usernameRegex = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)
)

// ...existing code...

func main() {
    // Load DSN from environment to avoid hard-coded credentials.
    // Example DSN format: "appuser:secret@tcp(db-host:3306)/appdb?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required (use a least-privileged DB user)")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    // Configure connection pool for defense-in-depth / least privilege behavior
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // verify connectivity with a short timeout
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db ping: %v", err)
    }

    // Background session cleanup
    go func() {
        t := time.NewTicker(5 * time.Minute)
        for range t.C {
            now := time.Now()
            sessions.Lock()
            for k, v := range sessions.m {
                if v.expires.Before(now) {
                    delete(sessions.m, k)
                }
            }
            sessions.Unlock()
        }
    }()

    http.HandleFunc("/login", loginHandler)
    // NOTE: production must run behind TLS (ListenAndServeTLS or reverse proxy). This example binds plain HTTP for dev only.
    log.Println("starting server on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server: %v", err)
    }
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (minimal set)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // CSP is recommended in production; omitted here for brevity.

    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 10<<10) // 10 KiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Input validation: strict whitelist for username, password presence check
    if !usernameRegex.MatchString(username) {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(password) == 0 {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    // Query the password hash with a short DB context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var storedHash string
    err := db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&storedHash)
    if err != nil {
        // Do not reveal whether username exists
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Compare password using bcrypt (resistant to timing attacks)
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // generic message on failure
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authentication successful: create a cryptographically secure session token
    token, err := newSessionToken(32)
    if err != nil {
        log.Printf("session token error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    expires := time.Now().Add(30 * time.Minute)

    // Store session in-memory (demo). Replace with Redis/DB in production.
    sessions.Lock()
    sessions.m[token] = sessionInfo{username: username, expires: expires}
    sessions.Unlock()

    // Set a secure cookie. Ensure HTTPS in production so Secure flag is effective.
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        Expires:  expires,
        HttpOnly: true,
        Secure:   true, // require TLS in production
        SameSite: http.SameSiteStrictMode,
    })

    // Successful response
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("ok"))
}

// newSessionToken returns a base64-url encoded cryptographically secure token.
func newSessionToken(n int) (string, error) {
    if n <= 0 {
        return "", errors.New("invalid token size")
    }
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// ...existing code...
