package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "net"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"

    mysql "github.com/go-sql-driver/mysql"
    _ "github.com/go-sql-driver/mysql"
)

var (
    db          *sql.DB
    usernameRe  = regexp.MustCompile(`^[a-zA-Z0-9_.-]{3,30}$`) // whitelist: allowed username chars
    rateLimiter = newIPRateLimiter(5, time.Minute)            // 5 attempts per minute per IP
)

// ...existing code...

func main() {
    // DB DSN must come from environment in production and use a least-privileged DB user.
    // Example: export DATABASE_DSN="username:password@tcp(localhost:3306)/appdb?parseTime=true&tls=false"
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        // Fail fast: do not run with embedded credentials
        fmt.Println("DATABASE_DSN env var is required")
        os.Exit(1)
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        fmt.Println("db open:", err)
        os.Exit(1)
    }
    // Set reasonable DB connection limits for least privilege / defense-in-depth
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // Verify DB connectivity early (with timeout)
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        fmt.Println("db ping:", err)
        os.Exit(1)
    }

    http.HandleFunc("/register", registerHandler)

    // NOTE: In production, terminate TLS at a reverse proxy or use ListenAndServeTLS.
    fmt.Println("listening :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        fmt.Println("server:", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Common security response headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("X-XSS-Protection", "0") // modern browsers prefer CSP; keep XSS filter off to avoid bypasses

    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 16<<10) // 16 KiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form", http.StatusBadRequest)
        return
    }

    // Basic IP-based rate limiting to slow brute force/account creation floods
    ip, _, _ := net.SplitHostPort(r.RemoteAddr)
    if !rateLimiter.Allow(ip) {
        http.Error(w, "too many requests", http.StatusTooManyRequests)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation: strict whitelist for usernames
    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Password policy: enforce minimum length and character variety
    if err := validatePassword(password); err != nil {
        // Don't reveal which rule failed to avoid giving attackers clues; but return generic message
        http.Error(w, "password does not meet complexity requirements", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (adaptive, salted). Use cost 12 as a reasonable default.
    const bcryptCost = 12
    hash, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // Do not reveal internal error details
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Store in DB with parameterized query and a short context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Prepared/parameterized statement prevents SQL injection
    query := `INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)`
    _, err = db.ExecContext(ctx, query, username, string(hash), time.Now().UTC())
    if err != nil {
        // Try to detect duplicate username (MySQL error 1062)
        var me *mysql.MySQLError
        if errors.As(err, &me) && me.Number == 1062 {
            // Avoid detailed error messages to prevent enumeration; use 409 Conflict
            http.Error(w, "user already exists", http.StatusConflict)
            return
        }
        // Generic error for other DB failures
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Success: created
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("account created"))
}

// validatePassword enforces a minimal password complexity policy.
// Keep rules strict; if requirements change, reflect in documentation and UX.
func validatePassword(p string) error {
    if len(p) < 12 {
        return fmt.Errorf("too short")
    }
    var hasLower, hasUpper, hasDigit, hasSpecial bool
    for _, ch := range p {
        switch {
        case 'a' <= ch && ch <= 'z':
            hasLower = true
        case 'A' <= ch && ch <= 'Z':
            hasUpper = true
        case '0' <= ch && ch <= '9':
            hasDigit = true
        default:
            // treat anything else as special; further normalization can be applied
            hasSpecial = true
        }
    }
    if !(hasLower && hasUpper && hasDigit && hasSpecial) {
        return fmt.Errorf("insufficient complexity")
    }
    return nil
}

// Simple in-memory IP rate limiter. For production, use a distributed rate limiter (Redis, etc.).
type ipRateLimiter struct {
    mtx       sync.Mutex
    limits    map[string]*ipCounter
    max       int
    window    time.Duration
    cleanupAt time.Time
}

type ipCounter struct {
    count int
    win   time.Time
}

func newIPRateLimiter(max int, window time.Duration) *ipRateLimiter {
    return &ipRateLimiter{
        limits: make(map[string]*ipCounter),
        max:    max,
        window: window,
    }
}

func (rl *ipRateLimiter) Allow(ip string) bool {
    rl.mtx.Lock()
    defer rl.mtx.Unlock()
    now := time.Now()
    e, ok := rl.limits[ip]
    if !ok || now.After(e.win.Add(rl.window)) {
        rl.limits[ip] = &ipCounter{count: 1, win: now}
        return true
    }
    if e.count >= rl.max {
        return false
    }
    e.count++
    return true
}
