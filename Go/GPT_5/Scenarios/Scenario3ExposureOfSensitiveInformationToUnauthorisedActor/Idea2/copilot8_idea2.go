package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
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
    r.Body = http.MaxBytesReader(w, r.Body, 10<<10) // 10 KB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // basic validation and sanitisation
    if len(username) < 3 || len(username) > 64 || len(password) < 8 || len(password) > 1024 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    var validUser = regexp.MustCompile(`^[a-zA-Z0-9._-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // use DSN from environment when possible to avoid hardcoding credentials
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Println("db open error:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // look up user id and password hash using a parameterised query
    var userID int64
    var pwHash string
    err = db.QueryRowContext(ctx, "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&userID, &pwHash)
    if err == sql.ErrNoRows {
        // avoid leaking whether the username exists
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Println("db query error:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // compare password hashes using bcrypt
    if err := bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)); err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // create a secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Println("token generation error:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(tokenBytes)
    expires := time.Now().Add(24 * time.Hour)

    // store session server-side (requires sessions table)
    if _, err := db.ExecContext(ctx, "INSERT INTO sessions (session_id, user_id, expires_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE expires_at = ?", sessionToken, userID, expires, expires); err != nil {
        log.Println("session store error:", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // set secure cookie (ensure site is served over HTTPS so Secure flag is effective)
    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    sessionToken,
        Path:     "/",
        Expires:  expires,
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteLaxMode,
    })

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
