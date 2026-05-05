package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "log"
    "net/http"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    var err error
    // Open once; this gives you a concurrency-safe connection pool
    // Add parseTime=true so DATETIME/DATE can round-trip into time.Time
    dsn := "root:root@tcp(localhost:3306)/db?parseTime=true"
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("unable to open database: %v", err)
    }
    // Verify we can connect
    if err = db.Ping(); err != nil {
        log.Fatalf("unable to ping database: %v", err)
    }

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler logs the user in if the username+password check out.
// On success it creates a session token in the DB and sets a secure cookie.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    // 1) only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST is allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) parse the form
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }
    username := r.Form.Get("username")
    password := r.Form.Get("password")
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // 3) Look up the user’s stored bcrypt hash
    var userID int
    var passwordHash string
    err := db.QueryRow(
        "SELECT id, password_hash FROM users WHERE username = ?",
        username,
    ).Scan(&userID, &passwordHash)

    if err == sql.ErrNoRows {
        // don’t reveal whether it was user or password that was wrong:
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("db error looking up user %q: %v", username, err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // 4) Compare the provided password against the stored hash
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // 5) Generate a new session token
    token, err := generateSessionToken()
    if err != nil {
        log.Printf("unable to generate session token: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    // expire in 24 hours
    expiresAt := time.Now().Add(24 * time.Hour)

    // 6) Store it in the DB
    _, err = db.Exec(
        "INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)",
        token, userID, expiresAt,
    )
    if err != nil {
        log.Printf("unable to store session in db: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // 7) Set a secure cookie
    cookie := http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        Expires:  expiresAt,
        HttpOnly: true,
        Secure:   true,                           // only over HTTPS
        SameSite: http.SameSiteStrictMode,        // or LaxMode
    }
    http.SetCookie(w, &cookie)

    // 8) success!
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}

// generateSessionToken returns a URL-safe, base64-encoded
// 32-byte cryptographically random token.
func generateSessionToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    // URL-safe base64 without padding
    return base64.RawURLEncoding.EncodeToString(b), nil
}