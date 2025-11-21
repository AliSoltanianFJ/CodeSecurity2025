// ...existing code...
package main

import (
    "context"
    "crypto/hmac"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "strconv"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB
var hmacSecret []byte
var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)

func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("missing DB_DSN")
    }
    secret := os.Getenv("HMAC_SECRET")
    if secret == "" {
        log.Fatal("missing HMAC_SECRET")
    }
    hmacSecret = []byte(secret)

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("db open error")
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal("db ping error")
    }

    http.HandleFunc("/login", loginHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "missing credentials", http.StatusBadRequest)
        return
    }
    if len(password) > 256 {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }
    if !usernameRE.MatchString(username) {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var (
        id           int64
        passwordHash string
    )
    err := db.QueryRowContext(ctx, "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&id, &passwordHash)
    if err != nil {
        // do not reveal whether user exists
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "unauthorized", http.StatusUnauthorized)
            return
        }
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "unauthorized", http.StatusUnauthorized)
        return
    }

    expiry := time.Now().Add(24 * time.Hour).Unix()
    payload := strconv.FormatInt(id, 10) + "|" + strconv.FormatInt(expiry, 10)
    mac := hmac.New(sha256.New, hmacSecret)
    _, _ = mac.Write([]byte(payload))
    sig := mac.Sum(nil)
    token := base64.RawURLEncoding.EncodeToString([]byte(payload)) + "." + base64.RawURLEncoding.EncodeToString(sig)

    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Unix(expiry, 0),
    }
    http.SetCookie(w, cookie)
    w.WriteHeader(http.StatusOK)
}
