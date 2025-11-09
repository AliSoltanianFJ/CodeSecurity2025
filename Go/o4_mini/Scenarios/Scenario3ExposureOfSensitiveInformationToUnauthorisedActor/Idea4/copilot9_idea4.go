package main

import (
    "context"
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "errors"
    "log"
    "net"
    "net/http"
    "os"
    "regexp"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var (
    db            *sql.DB
    sessionStore  = make(map[string]Session)
    sessionMux    sync.Mutex
    rateLimiter   = NewIPRateLimiter(5, 15*time.Minute) // 5 attempts per 15m
    usernameRegex = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)
)

type Session struct {
    UserID    int64
    ExpiresAt time.Time
}

func main() {
    // initialize DB using environment-provided DSN to avoid hard-coded credentials.
    // Example: export DB_DSN="appuser:strongpassword@tcp(localhost:3306)/appdb?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN not set in environment")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("db open failed: %v", err)
    }
    // Enforce reasonable connection limits and timeouts
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)

    // quick ping with timeout to validate connectivity at startup
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db ping failed: %v", err)
    }

    http.HandleFunc("/login", loginHandler)
    // In production, ListenAndServeTLS should be used with valid certs (TLS 1.3).
    // For testing only:
    log.Println("listening :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST for login
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic rate limiting per IP to mitigate brute force
    ip, _, _ := net.SplitHostPort(r.RemoteAddr)
    if !rateLimiter.Allow(ip) {
        http.Error(w, "Too many attempts, try later", http.StatusTooManyRequests)
        return
    }

    // Limit request body size to avoid large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 10<<10) // 10 KB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation: strict whitelist for usernames
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // Password length check (don't reveal specifics)
    if len(password) < 8 || len(password) > 1024 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var userID int64
    var passwordHash string

    // Parameterized query to avoid SQL injection
    row := db.QueryRowContext(ctx, "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1", username)
    if err := row.Scan(&userID, &passwordHash); err != nil {
        // Generic response to avoid user enumeration
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        // For unexpected DB errors, do not leak details
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password using bcrypt. Stored hashes must be generated with bcrypt.GenerateFromPassword.
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        // Wrong password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // At this point authentication succeeded. Create server-side session.
    token, err := generateSecureToken(32)
    if err != nil {
        log.Printf("token gen error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    expiry := time.Now().Add(24 * time.Hour)

    sessionMux.Lock()
    sessionStore[token] = Session{UserID: userID, ExpiresAt: expiry}
    sessionMux.Unlock()

    // Set secure cookie. In production ensure Secure=true with TLS.
    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        Expires:  expiry,
        HttpOnly: true,
        Secure:   true, // require TLS in production
        SameSite: http.SameSiteLaxMode,
    }
    http.SetCookie(w, cookie)

    // Return minimal response
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Logged in"))
}

// generateSecureToken returns a URL-safe base64 token
func generateSecureToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// IPRateLimiter is a simple in-memory rate limiter keyed by IP.
type IPRateLimiter struct {
    mu         sync.Mutex
    limits     map[string][]time.Time
    maxCount   int
    windowSize time.Duration
}

func NewIPRateLimiter(max int, window time.Duration) *IPRateLimiter {
    return &IPRateLimiter{
        limits:     make(map[string][]time.Time),
        maxCount:   max,
        windowSize: window,
    }
}

func (rl *IPRateLimiter) Allow(ip string) bool {
    rl.mu.Lock()
    defer rl.mu.Unlock()

    now := time.Now()
    windowStart := now.Add(-rl.windowSize)
    timestamps := rl.limits[ip]
    // keep only recent timestamps
    i := 0
    for ; i < len(timestamps); i++ {
        if timestamps[i].After(windowStart) {
            break
        }
    }
    timestamps = timestamps[i:]
    if len(timestamps) >= rl.maxCount {
        // deny
        rl.limits[ip] = timestamps
        return false
    }
    // allow and record
    timestamps = append(timestamps, now)
    rl.limits[ip] = timestamps
    return true
}

// ...existing code...
