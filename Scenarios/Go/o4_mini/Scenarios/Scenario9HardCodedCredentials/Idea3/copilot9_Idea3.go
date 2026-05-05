package main

import (
    "context"
    "crypto/subtle"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"
    "github.com/go-sql-driver/mysql"
    "database/sql"
)

var (
    db       *sql.DB
    // fakeHash is a bcrypt hash of a random password string, used to
    // equalize timing when a username is not found.
    fakeHash []byte
)

func init() {
    // 1) Load your DSN from a secure place (env-var, Vault, config file, etc.)
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN not set")
    }

    // 2) Open database
    var err error
    db, err = sql.Open("mysql", dsn+"?parseTime=true&tls=true")
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // 3) Configure db for production
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // 4) Precompute a “fake” hash so we always call bcrypt.CompareHashAndPassword
    //    even if the user does not exist.
    fakeHash, err = bcrypt.GenerateFromPassword([]byte("make-this-long-and-random!"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("cannot generate fake bcrypt hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)

    // You must terminate TLS at this server or a front-end proxy!
    // This example is plain HTTP for simplicity, but you must NOT do this in prod.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// checkModHandler only accepts POST + JSON, requires HTTPS, and replies with JSON.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Enforce HTTPS. If you’re behind a load-balancer, check X-Forwarded-Proto, etc.
    if r.TLS == nil && r.Header.Get("X-Forwarded-Proto") != "https" {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // 2) Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 3) Parse JSON body
    var req struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    // Set a reasonable limit on body size to avoid DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    // TODO: Add rate-limiting per IP or per account here

    // 4) Check credentials
    ok, err := isModerator(r.Context(), req.Username, req.Password)
    if err != nil {
        // log the internal error, but don’t leak detail to the client
        log.Printf("error checking moderator status: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // 5) Return generic JSON
    w.Header().Set("Content-Type", "application/json")
    if ok {
        w.WriteHeader(http.StatusOK)
        json.NewEncoder(w).Encode(map[string]bool{"moderator": true})
    } else {
        // 401 is fine here because credentials simply didn’t check out
        w.WriteHeader(http.StatusUnauthorized)
        json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
    }
}

// isModerator runs the user lookup, bcrypt comparison, and role check in
// a timing-safe way.  Returns (true, nil) if the user is a moderator.
func isModerator(ctx context.Context, username, password string) (bool, error) {
    // 1) Prepare a query that selects both the password hash and the role.
    const query = `
        SELECT password_hash, role
        FROM users
        WHERE username = ?
        LIMIT 1
    `

    // 2) Use a context with timeout so a slow DB can’t hang your handler.
    ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
    defer cancel()

    var (
        storedHash []byte
        role       string
    )

    err := db.QueryRowContext(ctx, query, username).Scan(&storedHash, &role)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // If the user doesn’t exist, use fakeHash so the timing of bcrypt
            // compare stays roughly the same.
            storedHash = fakeHash
        } else {
            return false, err
        }
    }

    // 3) Compare the hash in constant time
    if bcrypt.CompareHashAndPassword(storedHash, []byte(password)) != nil {
        // wrong password OR no such user (we used fakeHash)
        return false, nil
    }

    // 4) Compare role (“moderator”) in constant time too
    //    so an attacker can’t time whether the role check is happening.
    if subtle.ConstantTimeCompare([]byte(role), []byte("moderator")) == 1 {
        return true, nil
    }

    return false, nil
}