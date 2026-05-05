package main

import (
    "context"
    "crypto/tls"
    "database/sql"
    "encoding/json"
    "log"
    "net"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/lib/pq" // postgres driver
)

var (
    db         *sql.DB
    dummyHash  []byte
)

func init() {
    // Load a dummy bcrypt hash (cost=12) to use on missing users,
    // so that timing for “user not found” vs “bad password” is identical.
    var err error
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("dummy_password"), 12)
    if err != nil {
        log.Fatalf("unable to generate dummy bcrypt hash: %v", err)
    }

    // Build your DSN from env vars. Example for Postgres:
    //   export DB_HOST=…
    //   export DB_PORT=5432
    //   export DB_USER=…
    //   export DB_PASS=…
    //   export DB_NAME=…
    dsn := 
        "host=" + os.Getenv("DB_HOST") +
        " port=" + os.Getenv("DB_PORT") +
        " user=" + os.Getenv("DB_USER") +
        " password=" + os.Getenv("DB_PASS") +
        " dbname=" + os.Getenv("DB_NAME") +
        " sslmode=disable" // or=“require” in prod

    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("sql.Open failed: %v", err)
    }
    // Set sensible connection‐pool limits
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify we can connect
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.Ping failed: %v", err)
    }
}

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/check_mod", checkModHandler)

    // Load your TLS cert & key from env or secure volume
    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")
    if certFile == "" || keyFile == "" {
        log.Fatal("TLS_CERT_FILE and TLS_KEY_FILE must be set")
    }

    srv := &http.Server{
        Addr:         ":8443",
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
        TLSConfig: &tls.Config{
            MinVersion: tls.VersionTLS12,
        },
    }

    log.Printf("listening (TLS) on %s", srv.Addr)
    log.Fatal(srv.ListenAndServeTLS(certFile, keyFile))
}

// checkModHandler expects a JSON POST like { "username":"…", "password":"…" }
// and will reply with { "is_moderator":true } or { "is_moderator":false }.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Force a small body size to avoid DoS via huge payload
    r.Body = http.MaxBytesReader(w, r.Body, 1<<15) // 32kb
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()

    var req struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    if err := dec.Decode(&req); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    // Basic input length checks
    if len(req.Username) == 0 || len(req.Username) > 64 ||
       len(req.Password) == 0 || len(req.Password) > 128 {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    // Prepare our query
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var (
        storedHash   []byte
        isModerator  bool
    )
    err := db.QueryRowContext(ctx,
        `SELECT password_hash, is_moderator
           FROM users
          WHERE username = $1`, req.Username,
    ).Scan(&storedHash, &isModerator)

    switch {
    case err == sql.ErrNoRows:
        // user not found → do a dummy bcrypt compare to keep timing consistent
        bcrypt.CompareHashAndPassword(dummyHash, []byte(req.Password))
        // always say “not a moderator”
        writeJSON(w, false)
        return

    case err != nil:
        log.Printf("db error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // Compare password
    if bcrypt.CompareHashAndPassword(storedHash, []byte(req.Password)) != nil {
        // wrong password
        writeJSON(w, false)
        return
    }

    // OK, correct password
    writeJSON(w, isModerator)
}

func writeJSON(w http.ResponseWriter, isMod bool) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // prevent caching of auth responses
    w.Header().Set("Cache-Control", "no-store")
    w.WriteHeader(http.StatusOK)

    out := struct {
        IsModerator bool `json:"is_moderator"`
    }{isMod}
    json.NewEncoder(w).Encode(out)
}