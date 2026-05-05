package main

import (
    "crypto/rand"
    "context"
    "database/sql"
    "log"
    "encoding/hex"
    "encoding/json"
    "net/http"
    "os"
    "errors"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/login", loginHandler)

    // NOTE: run this behind a TLS-terminating reverse proxy (nginx, Caddy) with HTTP/2+ and HSTS.
    // The handler itself enforces that requests arrive over TLS; ListenAndServe is left for local dev only.
    log.Println("Starting server on :8080 (development). In production, terminate TLS upstream.")
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce minimal surface area
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce TLS (reject plain HTTP). In production, TLS should be terminated before this app
    if r.TLS == nil {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // Limit request body size to prevent abuse
    r.Body = http.MaxBytesReader(w, r.Body, 10<<10) // 10 KB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation: strict whitelist for username (adjust to your policy)
    var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)
    if !usernameRe.MatchString(username) || len(password) < 8 || len(password) > 128 {
        // Generic error to avoid user enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Get DB connection info from environment (do NOT hardcode credentials).
    // Expect DSN like: DB_DSN="appuser:secret@tcp(dbhost:3306)/dbname?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // Fail closed: do not attempt to continue without configured DB
        log.Println("DB_DSN not set")
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    // Configure connection pool for least privilege and stability
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)
    defer db.Close()

    // Use a short context timeout to avoid hanging requests (DoS mitigation)
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    // Parameterized query to avoid SQL injection; assume column 'password_hash' stores bcrypt hashes.
    const query = "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    var passwordHash string
    err = db.QueryRowContext(ctx, query, username).Scan(&passwordHash)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // Do not reveal whether username exists
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Println("db query:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Verify password using bcrypt. bcrypt.CompareHashAndPassword is safe against timing leaks for this use.
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        // Authentication failed
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authentication successful. Create a session identifier (cryptographically random).
    sessionID, err := newSessionID(32)
    if err != nil {
        log.Println("session id generation:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // TODO: Persist sessionID server-side (in DB or cache) with expiration and associate with user.
    // For this example, we set a secure cookie and instruct to store server-side session mapping.
    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // requires TLS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    // Security response headers (basic set)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Minimal JSON success payload (no sensitive data)
    resp := map[string]string{"status": "ok"}
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(resp)
}

// newSessionID returns a hex-encoded securely generated random ID of n bytes.
func newSessionID(n int) (string, error) {
    if n <= 0 {
        return "", errors.New("invalid length")
    }
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}