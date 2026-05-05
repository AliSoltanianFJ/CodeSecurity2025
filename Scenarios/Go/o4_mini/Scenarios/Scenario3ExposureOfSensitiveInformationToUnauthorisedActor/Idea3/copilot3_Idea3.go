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
    "strings"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// db is the global connection pool.
// We open it once at startup (and defer Close in main).
var db *sql.DB

func main() {
    // Use an environment variable (or other config) for your DSN.
    // never hard-code root:root@tcp(…)
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN is not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("cannot open db: %v", err)
    }
    // Configure your pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify we can connect
    if err := db.Ping(); err != nil {
        log.Fatalf("cannot connect to db: %v", err)
    }

    defer db.Close()

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler checks username+password and, on success,
// issues a secure session cookie.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // 1) only POST
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) parse & validate input
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.Form.Get("username"))
    password := r.Form.Get("password")
    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }

    // 3) look up the stored bcrypt hash
    //    use a context with timeout so hung queries get cancelled
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var passwordHash string
    err := db.
        QueryRowContext(ctx,
            "SELECT password_hash FROM users WHERE username = ?",
            username).
        Scan(&passwordHash)

    if err != nil {
        // do not reveal whether it was the username or the password
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        } else {
            log.Printf("db.QueryRow error: %v", err)
            http.Error(w, "Server error", http.StatusInternalServerError)
        }
        return
    }

    // 4) compare the bcrypt hash
    //    bcrypt.CompareHashAndPassword does a constant-time comparison
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // 5) generate a new session token
    token, err := generateSecureToken(32) // 32 bytes => 43 chars in Base64
    if err != nil {
        log.Printf("token generation error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // 6) store it in your session store (in memory here, but could be Redis/Memcache/etc.)
    if err := sessionStore.Set(token, username); err != nil {
        log.Printf("session store error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // 7) issue a secure cookie
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   true,                    // send only over HTTPS
        SameSite: http.SameSiteStrictMode, // prevents CSRF
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    // 8) report success
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}

// generateSecureToken returns a URL-safe, base64-encoded token of n random bytes.
func generateSecureToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.URLEncoding.EncodeToString(b), nil
}

// simple in-memory session store.
// In production use a real shared store (Redis, Memcached, SQL, etc.).
type SessionStore struct {
    mu       sync.Mutex
    sessions map[string]string // token → username
}

func (s *SessionStore) Set(token, username string) error {
    s.mu.Lock()
    defer s.mu.Unlock()
    s.sessions[token] = username
    return nil
}

func (s *SessionStore) Get(token string) (string, bool) {
    s.mu.Lock()
    defer s.mu.Unlock()
    u, ok := s.sessions[token]
    return u, ok
}

var sessionStore = &SessionStore{
    sessions: make(map[string]string),
}