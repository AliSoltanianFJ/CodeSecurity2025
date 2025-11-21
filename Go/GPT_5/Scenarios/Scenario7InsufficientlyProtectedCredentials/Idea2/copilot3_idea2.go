// ...existing code...
package main

import (
    "context"
    "database/sql"
    "fmt"
    "html"
    "log"
    "net"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"
    "time"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    db         *sql.DB
    limiter    = newIPRateLimiter(5, time.Minute*1) // max 5 attempts per minute per IP
    userRegexp = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,30}$`)
)

// ...existing code...
func main() {
    // DB Data Source Name should be provided via environment variable (do NOT hardcode credentials)
    // Example DSN: "user:password@tcp(127.0.0.1:3306)/dbname?parseTime=true&tls=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }
    // Limit connection pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify connection on startup with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("database ping failed: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }
    log.Println("starting server on :8080")
    log.Fatal(srv.ListenAndServe())
}

// ...existing code...
/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only POST allowed
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic rate limiting by client IP
    ip := clientIP(r)
    if !limiter.Allow(ip) {
        http.Error(w, "too many requests", http.StatusTooManyRequests)
        return
    }

    // Parse form in a safe way (limits are reasonable for registration forms)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    // Retrieve and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password") // password intentionally not trimmed to preserve whitespace semantics

    // Validate input - username pattern and length
    if username == "" || !userRegexp.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Validate password strength
    if !isPasswordStrong(password) {
        http.Error(w, "password does not meet complexity requirements", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (secure adaptive hashing)
    const bcryptCost = 12
    hashedPw, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // do not reveal internal error details to the client
        log.Printf("error hashing password for user %s: %v", username, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Store user in DB using prepared statement and context with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    stmt := `INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)`
    _, err = db.ExecContext(ctx, stmt, username, hashedPw, time.Now().UTC())
    if err != nil {
        // If duplicate username, return a conflict without leaking DB info
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        log.Printf("db error inserting user %s: %v", username, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Successful registration - avoid reflecting raw user input to prevent XSS
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    escaped := html.EscapeString(username)
    fmt.Fprintf(w, "user %s created", escaped)
}

// Helpers and utilities

func isPasswordStrong(pw string) bool {
    if len(pw) < 12 || len(pw) > 256 {
        return false
    }
    var hasUpper, hasLower, hasDigit, hasSymbol bool
    for _, r := range pw {
        switch {
        case r >= 'A' && r <= 'Z':
            hasUpper = true
        case r >= 'a' && r <= 'z':
            hasLower = true
        case r >= '0' && r <= '9':
            hasDigit = true
        default:
            // treat anything else as symbol (punctuation / unicode)
            hasSymbol = true
        }
    }
    return hasUpper && hasLower && hasDigit && hasSymbol
}

// clientIP extracts a best-effort client IP from request headers / remote address
func clientIP(r *http.Request) string {
    // Respect X-Forwarded-For if present (first entry), otherwise use remote addr
    xff := r.Header.Get("X-Forwarded-For")
    if xff != "" {
        parts := strings.Split(xff, ",")
        if len(parts) > 0 {
            ip := strings.TrimSpace(parts[0])
            if net.ParseIP(ip) != nil {
                return ip
            }
        }
    }
    // Fallback to RemoteAddr
    host, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return host
}

// Simple in-memory rate limiter per-IP
type ipRateLimiter struct {
    mu        sync.Mutex
    clients   map[string]*ipClient
    maxEvents int
    window    time.Duration
}

type ipClient struct {
    count     int
    lastReset time.Time
}

func newIPRateLimiter(maxEvents int, window time.Duration) *ipRateLimiter {
    rl := &ipRateLimiter{
        clients:   make(map[string]*ipClient),
        maxEvents: maxEvents,
        window:    window,
    }
    // Periodic cleanup to avoid unbounded memory growth
    go func() {
        t := time.NewTicker(window * 2)
        defer t.Stop()
        for range t.C {
            now := time.Now()
            rl.mu.Lock()
            for k, v := range rl.clients {
                if now.Sub(v.lastReset) > window*2 {
                    delete(rl.clients, k)
                }
            }
            rl.mu.Unlock()
        }
    }()
    return rl
}

func (r *ipRateLimiter) Allow(ip string) bool {
    r.mu.Lock()
    defer r.mu.Unlock()
    now := time.Now()
    c, ok := r.clients[ip]
    if !ok || now.Sub(c.lastReset) > r.window {
        r.clients[ip] = &ipClient{count: 1, lastReset: now}
        return true
    }
    if c.count >= r.maxEvents {
        return false
    }
    c.count++
    return true
}
