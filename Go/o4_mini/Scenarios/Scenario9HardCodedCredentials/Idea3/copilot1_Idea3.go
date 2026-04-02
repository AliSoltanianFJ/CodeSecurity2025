package main

import (
    "crypto/subtle"
    "database/sql"
    "encoding/json"
    "errors"
    "fmt"
    "log"
    "net/http"
    "os"
    "time"

    _ "github.com/lib/pq"             // or whatever driver you use
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func init() {
    // open your DB in init()
    // e.g.   postgres://user:pass@host/dbname?sslmode=verify-full
    dsn := os.Getenv("DATABASE_URL")
    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }
    // use sensible connection limits for your workload
    db.SetMaxOpenConns(10)
    db.SetConnMaxIdleTime(5 * time.Minute)
}

// User holds the minimal info we need
type User struct {
    Username     string
    PasswordHash string
    IsModerator  bool
}

// checkModHandler only accepts HTTPS POSTs with a small JSON payload.
// It writes a JSON response {"is_mod":true} or {"is_mod":false}.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow HTTPS
    if r.TLS == nil {
        http.Error(w, "use HTTPS", http.StatusUpgradeRequired)
        return
    }

    // 2) Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 3) Only accept application/json
    if !ctIsJSON(r.Header.Get("Content-Type")) {
        http.Error(w, "Content-Type must be application/json", http.StatusBadRequest)
        return
    }

    // 4) Parse and limit the size of the body
    var creds struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    dec := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<10)) // 1KB max
    if err := dec.Decode(&creds); err != nil {
        http.Error(w, "invalid JSON", http.StatusBadRequest)
        return
    }

    // 5) Verify credentials and check mod flag
    isMod, err := isModerator(creds.Username, creds.Password)
    if err != nil {
        // On any error: return 401 but do NOT leak whether the user exists or not
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    // 6) Return the boolean result
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, `{"is_mod":%t}`, isMod)
}

// isModerator looks up the user by username, compares the password
// using bcrypt, and returns true if the credentials are valid AND
// the user has their IsModerator flag set.
func isModerator(username, password string) (bool, error) {
    var u User

    // Prepared statement to avoid SQL injection
    err := db.QueryRow(`
        SELECT username, password_hash, is_moderator
        FROM users
        WHERE username = $1
    `, username).Scan(&u.Username, &u.PasswordHash, &u.IsModerator)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // Do a fake bcrypt hash compare to equalize timing
            fakeHashCompare()
            return false, fmt.Errorf("user not found")
        }
        return false, err
    }

    // CompareHashAndPassword runs in constant time
    if bcrypt.CompareHashAndPassword([]byte(u.PasswordHash), []byte(password)) != nil {
        return false, fmt.Errorf("password mismatch")
    }

    // At this point credentials are valid; return the mod flag
    return u.IsModerator, nil
}

// fakeHashCompare runs one bcrypt hash/compare cycle on a constant,
// dummy password.  This prevents an attacker from timing the difference
// between “user not found” vs “password wrong.”
func fakeHashCompare() {
    // bcrypt.DefaultCost on a fixed string
    dummy := "$2a$10$N9qo8uLOickgx2ZMRZo5i.u5YWZkxlr/5D4aqYv50aS0UM.9uWQGa" // hash of "passw0rd"
    _ = bcrypt.CompareHashAndPassword([]byte(dummy), []byte("secret"))
}

// ctIsJSON is a very simple Content-Type check.
func ctIsJSON(ct string) bool {
    // You may wish to be more liberal and accept charset etc.
    return ct == "application/json"
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)

    // Always serve TLS in production. For local dev, you might use a self-signed cert.
    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")
    log.Printf("listening on :8443 …")
    log.Fatal(http.ListenAndServeTLS(":8443", certFile, keyFile, nil))
}