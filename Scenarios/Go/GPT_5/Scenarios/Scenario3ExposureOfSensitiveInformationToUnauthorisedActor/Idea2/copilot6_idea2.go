package main

import (
    "context"
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // protect against very large bodies
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // basic input validation and sanitisation
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(username) > 64 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    // allow common username/email characters only
    if ok, _ := regexp.MatchString(`^[A-Za-z0-9@._-]{1,64}$`, username); !ok {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // open DB with reasonable defaults and short context timeout
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var hashedPassword string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        // do not reveal whether username exists
        if err == sql.ErrNoRows {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // compare using bcrypt (constant-time)
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // create a short-lived HMAC-signed session token (requires SESSION_KEY env var)
    secret := os.Getenv("SESSION_KEY")
    if secret == "" {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    nonce := make([]byte, 32)
    if _, err := rand.Read(nonce); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write(nonce)
    mac.Write([]byte(username))
    sig := mac.Sum(nil)

    token := base64.RawURLEncoding.EncodeToString(append(nonce, sig...))

    // set secure cookie (ensure TLS in production)
    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // requires HTTPS in production
        SameSite: http.SameSiteStrictMode,
        MaxAge:   3600,
    })

    // generic success response (no sensitive info returned)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
