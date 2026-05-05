package main

import (
    "context"
    "crypto/tls"
    "encoding/json"
    "errors"
    "log"
    "net"
    "net/http"
    "os"
    "sync"
    "time"

    "database/sql"
    _ "github.com/lib/pq"

    "golang.org/x/crypto/bcrypt"
    "golang.org/x/time/rate"
)

var (
    db        *sql.DB
    dummyHash []byte // a bcrypt hash of some fixed string, used to defend against timing attacks on nonexistent users
)

func init() {
    // Precompute a dummy bcrypt hash.  We only do this once so
    // that CompareHashAndPassword for unknown usernames still
    // costs the same as for real users.
    var err error
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("dummy password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("could not generate dummy bcrypt hash: %v", err)
    }

    // Open a connection pool to your database.  Adjust connection
    // settings (max open, max idle, etc.) to match your use case.
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL environment variable is not set")
    }
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // Optional: tune these
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify we can connect
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }
}

func main() {
    mux := http.NewServeMux()
    mux.Handle("/check_mod", rateLimitMiddleware(1, 5)(http.HandlerFunc(checkModHandler)))

    // Always serve HTTPS.  Replace with paths to your cert & key.
    server := &http.Server{
        Addr:         ":443",
        Handler:      hstsMiddleware(mux),
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
        TLSConfig: &tls.Config{
            MinVersion:               tls.VersionTLS12,
            PreferServerCipherSuites: true,
        },
    }

    log.Printf("listening on https://%s …", server.Addr)
    log.Fatal(server.ListenAndServeTLS("server.crt", "server.key"))
}

// request /check_mod with POST {"username":"…","password":"…"}
// response: 200 OK {"is_mod":true} or {"is_mod":false}
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow POST + JSON
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if ct := r.Header.Get("Content-Type"); ct != "application/json" {
        http.Error(w, "content-type must be application/json", http.StatusBadRequest)
        return
    }

    // 2) Decode credentials
    type credentials struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    var creds credentials
    // limit to 1MB just in case
    dec := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20))
    if err := dec.Decode(&creds); err != nil {
        http.Error(w, "invalid request body", http.StatusBadRequest)
        return
    }
    if creds.Username == "" || creds.Password == "" {
        http.Error(w, "username & password required", http.StatusBadRequest)
        return
    }

    // 3) Query the database with a context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var storedHash []byte
    var isMod bool

    err := db.QueryRowContext(ctx,
        "SELECT password_hash, is_mod FROM users WHERE username = $1",
        creds.Username,
    ).Scan(&storedHash, &isMod)

    switch {
    case errors.Is(err, sql.ErrNoRows):
        // user not found → run one dummy bcrypt compare to keep timing roughly constant
        _ = bcrypt.CompareHashAndPassword(dummyHash, []byte(creds.Password))
        // and simply return is_mod = false
        respondJSON(w, http.StatusOK, map[string]bool{"is_mod": false})
        return

    case err != nil:
        // real database error
        log.Printf("db error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 4) Compare the bcrypt hashes (this is already a constant-time operation)
    if err := bcrypt.CompareHashAndPassword(storedHash, []byte(creds.Password)); err != nil {
        // password mismatch
        respondJSON(w, http.StatusOK, map[string]bool{"is_mod": false})
        return
    }

    // 5) Success
    respondJSON(w, http.StatusOK, map[string]bool{"is_mod": isMod})
}

// tiny helper to send a JSON response
func respondJSON(w http.ResponseWriter, status int, payload interface{}) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(payload)
}

// Simple HSTS middleware
func hstsMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains")
        next.ServeHTTP(w, r)
    })
}

// rateLimitMiddleware returns a middleware that rate‐limits each
// remote IP to rate r with a burst of b.
func rateLimitMiddleware(r rate.Limit, b int) func(http.Handler) http.Handler {
    // keep one limiter per IP
    var (
        mu       sync.Mutex
        clients  = make(map[string]*rate.Limiter)
        cleanup  = time.NewTicker(10 * time.Minute)
    )
    // garbage‐collect stale IPs periodically
    go func() {
        for range cleanup.C {
            mu.Lock()
            for ip, lim := range clients {
                // if no tokens have been consumed in a while, drop it
                if lim.ReserveN(time.Now(), b).Delay() > time.Hour {
                    delete(clients, ip)
                }
            }
            mu.Unlock()
        }
    }()

    return func(next http.Handler) http.Handler {
        return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
            ip, _, err := net.SplitHostPort(r.RemoteAddr)
            if err != nil {
                http.Error(w, "internal error", http.StatusInternalServerError)
                return
            }

            mu.Lock()
            lim, exists := clients[ip]
            if !exists {
                lim = rate.NewLimiter(r, b)
                clients[ip] = lim
            }
            mu.Unlock()

            if !lim.Allow() {
                http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
                return
            }
            next.ServeHTTP(w, r)
        })
    }
}