// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

type sessionData struct {
    Username string
    Expiry   time.Time
}

var (
    db       *sql.DB
    sessions sync.Map
    userRe   = regexp.MustCompile(`^[A-Za-z0-9_]{3,64}$`)
)

// ...existing code...
func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("failed to open database")
    }
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal("failed to connect to database")
    }

    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    if !userRe.MatchString(username) {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(password) < 8 || len(password) > 256 {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var passwordHash string
    err := db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username=? LIMIT 1", username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("db query error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    token, err := newSessionToken(32)
    if err != nil {
        log.Printf("token generation error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    expiry := time.Now().Add(24 * time.Hour)
    sessions.Store(token, sessionData{Username: username, Expiry: expiry})

    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        Expires:  expiry,
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
    })
    w.Header().Set("Content-Type", "application/json")
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func newSessionToken(n int) (string, error) {
    buf := make([]byte, n)
    if _, err := rand.Read(buf); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(buf), nil
}