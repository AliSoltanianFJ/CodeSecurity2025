package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "log"
    "net/http"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    // db is our global connection‐pool
    db *sql.DB

    // sessionStore is a very simple in‐memory map of sessionToken→username.
    // In production you’d use Redis or a sessions table in MySQL, etc.
    sessionStore = make(map[string]string)
    sessionMu    sync.Mutex
)

func main() {
    var err error
    // Open once at startup; this does not actually connect until we use it,
    // but it manages a pool of connections for us.
    dsn := "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4"
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("unable to open db: %v", err)
    }
    // Optional tuning of the pool:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify we can connect at all.
    if err = db.Ping(); err != nil {
        log.Fatalf("unable to ping db: %v", err)
    }

    http.HandleFunc("/login", loginHandler)

    // In production you’d run behind TLS.  For testing over HTTP you can
    // still set Secure:false on the cookie, but never do that in prod.
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler authenticates a user by username+password.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST is allowed", http.StatusMethodNotAllowed)
        return
    }

    // ParseForm is called internally by FormValue, but you can also
    // call r.ParseForm() here if you need to check r.PostForm directly.
    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // 1) Look up the stored bcrypt hash for this user
    var storedHash string
    query := "SELECT password_hash FROM users WHERE username = ?"
    err := db.QueryRow(query, username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        // We do not reveal “user doesn’t exist” vs “bad password”
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("DB error looking up user %q: %v", username, err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 2) Compare the supplied password with the stored bcrypt hash
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // wrong password
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    // 3) Password is correct → create a new session token
    token, err := generateSessionToken(32)
    if err != nil {
        log.Printf("error generating session token: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 4) Store the mapping sessionToken→username
    sessionMu.Lock()
    sessionStore[token] = username
    sessionMu.Unlock()

    // 5) Set a cookie
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        Expires:  time.Now().Add(24 * time.Hour),
        HttpOnly: true,
        Secure:   true,                         // only send over HTTPS in production
        SameSite: http.SameSiteStrictMode,      // adjust to your needs
    }
    http.SetCookie(w, cookie)

    w.Write([]byte("login successful"))
}

// generateSessionToken returns a URL‐safe, base64‐encoded random token.
func generateSessionToken(nBytes int) (string, error) {
    b := make([]byte, nBytes)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    // base64.RawURLEncoding instead of URLEncoding to avoid padding "="
    return base64.RawURLEncoding.EncodeToString(b), nil
}