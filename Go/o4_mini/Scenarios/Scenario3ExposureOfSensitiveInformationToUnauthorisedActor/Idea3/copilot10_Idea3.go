package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "errors"
    "golang.org/x/crypto/bcrypt"
    "log"
    "net/http"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

var (
    // db is our long-lived connection pool.
    db *sql.DB

    // ErrInvalidCredentials is returned when username/password are wrong.
    ErrInvalidCredentials = errors.New("invalid credentials")
)

func main() {
    // Read DSN from env for flexibility and security
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Set sensible limits on your pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify we can talk to the database
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("db.PingContext: %v", err)
    }

    http.HandleFunc("/login", loginHandler)

    addr := ":8080"
    log.Printf("listening on %s …", addr)
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// loginHandler logs in a user, sets a secure session cookie on success.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // ParseForm is cheap and idempotent; it populates r.Form
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.Form.Get("username")
    password := r.Form.Get("password")
    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }

    // Authenticate the user
    userID, err := authenticate(r.Context(), username, password)
    if err == ErrInvalidCredentials {
        // Always return 401 on bad creds; do NOT leak whether user exists
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Printf("authenticate error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Create a new session
    token, expires, err := createSession(r.Context(), userID)
    if err != nil {
        log.Printf("createSession error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Set session cookie
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",              // available to entire site
        Expires:  expires,
        HttpOnly: true,             // no JS access
        Secure:   true,             // set to true in production (https only)
        SameSite: http.SameSiteLaxMode,
    }
    http.SetCookie(w, cookie)

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}

// authenticate checks the username and password, returning the user’s ID.
func authenticate(ctx context.Context, username, password string) (int, error) {
    // We give ourselves a timeout for the DB call
    ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
    defer cancel()

    // 1) Look up the user’s bcrypt password hash
    var (
        hashedPassword string
        userID         int
    )
    err := db.QueryRowContext(
        ctx,
        "SELECT id, password_hash FROM users WHERE username = ?",
        username,
    ).Scan(&userID, &hashedPassword)
    if err == sql.ErrNoRows {
        return 0, ErrInvalidCredentials
    }
    if err != nil {
        return 0, err
    }

    // 2) Compare the supplied password to the stored hash
    if err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)); err != nil {
        // this covers wrong password AND invalid hash formats
        return 0, ErrInvalidCredentials
    }

    return userID, nil
}

// createSession generates a new session token, stores it, and returns its value and expiry.
func createSession(ctx context.Context, userID int) (string, time.Time, error) {
    // 32 random bytes => 44 chars base64 URL-encoded
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", time.Time{}, err
    }
    token := base64.RawURLEncoding.EncodeToString(b)
    expires := time.Now().Add(24 * time.Hour)

    ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
    defer cancel()

    // Insert into sessions table
    _, err := db.ExecContext(
        ctx,
        "INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)",
        token, userID, expires,
    )
    if err != nil {
        return "", time.Time{}, err
    }

    return token, expires, nil
}