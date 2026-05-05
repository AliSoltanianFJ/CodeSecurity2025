package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
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

var (
    db           *sql.DB
    usernameRe   = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)
    sessions     = make(map[string]session)
    sessionsLock sync.Mutex
)

type session struct {
    User   string
    Expiry time.Time
}

func main() {
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("missing MYSQL_DSN environment variable")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("failed to open database")
    }
    if err = db.Ping(); err != nil {
        log.Fatal("failed to connect to database")
    }

    http.HandleFunc("/login", loginHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if err := r.ParseForm(); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var passwordHash string
    err := db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("db query error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    token, err := generateToken(32)
    if err != nil {
        log.Printf("token generation error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    expiry := time.Now().Add(24 * time.Hour)
    sessionsLock.Lock()
    sessions[token] = session{User: username, Expiry: expiry}
    sessionsLock.Unlock()

    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   r.TLS != nil,
        SameSite: http.SameSiteStrictMode,
        Expires:  expiry,
    }
    http.SetCookie(w, cookie)

    w.Header().Set("Content-Type", "application/json")
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}

func generateToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
