package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
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
    db  *sql.DB
    rxUser = regexp.MustCompile(`^[a-zA-Z0-9_.-]{3,30}$`)
    // simple in-memory rate limiting (per-IP). For prod use redis/ratelimit middleware.
    rl = struct {
        sync.Mutex
        m map[string][]time.Time
    }{m: make(map[string][]time.Time)}
)

func main() {
    // DB DSN must come from environment variable; use least-privilege DB user.
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        log.Fatal("DATABASE_DSN env required")
    }
    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    // set reasonable connection pool limits (least privilege, resource constraints)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db ping: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    // In production run behind TLS reverse proxy. HSTS header below enforces HTTPS.
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (defense-in-depth)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")
    // HSTS: only effective when served over TLS
    w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    // Basic anti-CSRF requirement: require a custom header (caller must implement proper CSRF tokens)
    // Production: implement server-generated CSRF tokens or SameSite cookies + double-submit.
    if r.Header.Get("X-CSRF-Token") == "" {
        http.Error(w, "Missing CSRF token", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Validate inputs (whitelist)
    if !rxUser.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 12 {
        http.Error(w, "Password too short; minimum 12 characters", http.StatusBadRequest)
        return
    }
    // Additional password strength checks recommended (entropy, banned-password list).

    // Rate limiting per IP to mitigate brute-force and abuse.
    ip := getIP(r)
    if blocked := rateLimitCheck(ip, 5, time.Minute); blocked {
        http.Error(w, "Too many requests", http.StatusTooManyRequests)
        return
    }

    // Hash password using bcrypt (secure, includes salt). Use cost from env if provided.
    cost := bcrypt.DefaultCost
    if c := os.Getenv("BCRYPT_COST"); c != "" {
        // ignore parsing error - stick with default if invalid
        if parsed, err := strconv.Atoi(c); err == nil && parsed >= 10 && parsed <= 16 {
            cost = parsed
        }
    }
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), cost)
    if err != nil {
        // Do not reveal internals to client
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Store user using parameterized query; enforce DB timeout and least privilege DB user.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    tx, err := db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelReadCommitted})
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    stmt, err := tx.PrepareContext(ctx, `INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)`)
    if err != nil {
        tx.Rollback()
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    if _, err := stmt.ExecContext(ctx, username, string(hashed), time.Now().UTC()); err != nil {
        tx.Rollback()
        // avoid revealing if username exists; map DB errors appropriately
        if isDuplicateEntryErr(err) {
            http.Error(w, "Account already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    if err := tx.Commit(); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Success: do not return sensitive data
    w.WriteHeader(http.StatusCreated)
    fmt.Fprint(w, "registered")
}

// helper: simple per-IP sliding window rate limiter
func rateLimitCheck(ip string, max int, window time.Duration) bool {
    rl.Lock()
    defer rl.Unlock()
    now := time.Now()
    cutoff := now.Add(-window)
    times := rl.m[ip]
    // drop old timestamps
    newTimes := make([]time.Time, 0, len(times))
    for _, t := range times {
        if t.After(cutoff) {
            newTimes = append(newTimes, t)
        }
    }
    newTimes = append(newTimes, now)
    rl.m[ip] = newTimes
    return len(newTimes) > max
}

// helper: best-effort IP extraction
func getIP(r *http.Request) string {
    // Prefer X-Forwarded-For if behind trusted proxy; validate trusted proxy in deployment.
    if xf := r.Header.Get("X-Forwarded-For"); xf != "" {
        // take first entry
        if idx := strings.Index(xf, ","); idx >= 0 {
            return strings.TrimSpace(xf[:idx])
        }
        return strings.TrimSpace(xf)
    }
    ip, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return ip
}

// helper: detect duplicate entry in MySQL (ER_DUP_ENTRY = 1062)
func isDuplicateEntryErr(err error) bool {
    var mysqlErr *mysql.MySQLError
    if errors.As(err, &mysqlErr) {
        if mysqlErr.Number == 1062 {
            return true
        }
    }
    return false
}

// ...existing code...
