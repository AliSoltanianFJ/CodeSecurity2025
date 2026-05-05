package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "errors"
    "fmt"
    "log"
    "net"
    "net/http"
    "os"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"     // or pq, etc.
    "golang.org/x/crypto/bcrypt"
    "golang.org/x/time/rate"
)

// User represents the result row we need.
type User struct {
    PasswordHash string // bcrypt hash from DB
    IsModerator  bool   // true if this user is a moderator
}

// global DB handle
var db *sql.DB

func main() {
    // Pull your DSN from an env var or secret store
    dsn := os.Getenv("MYAPP_DSN")
    if dsn == "" {
        log.Fatal("MYAPP_DSN not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn+"?parseTime=true&tls=true")
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    defer db.Close()

    // Ping DB to verify connectivity
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/check_mod", checkModHandler)

    // In production you’ll terminate TLS in front of this,
    // e.g. with nginx / load-balancer / cert-manager, etc.
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// rateLimiter per‐IP to slow brute-force abuses
var (
    limiters   = make(map[string]*rate.Limiter)
    limiterMu  sync.Mutex
    rps        = rate.Limit(1)  // 1 request per second
    burst      = 3              // allow 3 requests in a burst
    blockAfter = 5 * time.Minute
)

func getRateLimiter(ip string) *rate.Limiter {
    limiterMu.Lock()
    defer limiterMu.Unlock()

    if l, exists := limiters[ip]; exists {
        return l
    }
    l := rate.NewLimiter(rps, burst)
    limiters[ip] = l
    // Optionally add cleanup of old entries
    return l
}

// checkModHandler authenticates a user and returns { "moderator": true|false }.
// It never leaks more detail than “401 Unauthorized” or “403 Forbidden.”
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow HTTPS in production.
    if r.TLS == nil {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // 2) Only allow POST + JSON
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if ct := r.Header.Get("Content-Type"); ct != "application/json" {
        http.Error(w, "Content-Type must be application/json", http.StatusBadRequest)
        return
    }

    // 3) Rate limit by client IP
    ip, _, _ := net.SplitHostPort(r.RemoteAddr)
    limiter := getRateLimiter(ip)
    if !limiter.Allow() {
        // 429 Too Many Requests
        http.Error(w, "Too many requests", http.StatusTooManyRequests)
        return
    }

    // 4) Decode request JSON
    type creds struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    var c creds
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&c); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    defer r.Body.Close()

    // 5) Validate inputs (e.g. length checks, allowed charset, etc.)
    if len(c.Username) < 3 || len(c.Username) > 64 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(c.Password) < 8 || len(c.Password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // 6) Authenticate
    isMod, err := checkIfModerator(r.Context(), c.Username, c.Password)
    if err != nil {
        switch {
        case errors.Is(err, sql.ErrNoRows):
            fallthrough
        case errors.Is(err, bcrypt.ErrMismatchedHashAndPassword):
            // avoid distinguishing “no such user” vs “bad pwd”
            http.Error(w, "Unauthorized", http.StatusUnauthorized)
        default:
            log.Printf("internal error checking moderator: %v", err)
            http.Error(w, "Internal server error", http.StatusInternalServerError)
        }
        return
    }

    // 7) Return JSON result
    w.Header().Set("Content-Type", "application/json")
    if isMod {
        w.WriteHeader(http.StatusOK)
    } else {
        w.WriteHeader(http.StatusForbidden)
    }
    json.NewEncoder(w).Encode(map[string]bool{"moderator": isMod})
}

// checkIfModerator returns (true, nil) if username/password is valid AND user is a moderator.
// On bcrypt mismatch or missing user, it returns an error so caller returns 401.
func checkIfModerator(ctx context.Context, username, password string) (bool, error) {
    // short timeout so a hung DB doesn’t block everything
    ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
    defer cancel()

    var u User
    // Parameterized query prevents SQL injection
    const q = `
      SELECT password_hash, is_mod
        FROM users
       WHERE username = ?
    `
    err := db.QueryRowContext(ctx, q, username).Scan(&u.PasswordHash, &u.IsModerator)
    if err != nil {
        return false, err
    }

    // bcrypt.CompareHashAndPassword is safe from timing attacks
    if err := bcrypt.CompareHashAndPassword([]byte(u.PasswordHash), []byte(password)); err != nil {
        return false, err
    }

    return u.IsModerator, nil
}