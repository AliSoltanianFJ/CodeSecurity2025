package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
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

    // Parse the form safely
    if err := r.ParseForm(); err != nil {
        log.Println("form parse error:", err)
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic validation & sanitisation
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    // Allow only alphanumeric and underscore usernames of reasonable length
    if ok, _ := regexp.MatchString(`^[A-Za-z0-9_]{3,32}$`, username); !ok {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    // Enforce a reasonable password length (do not reflect the password back)
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Build DSN from environment variables to avoid hardcoding secrets in source
    dbUser := os.Getenv("DB_USER")
    dbPass := os.Getenv("DB_PASS")
    dbHost := os.Getenv("DB_HOST")
    dbName := os.Getenv("DB_NAME")
    if dbHost == "" {
        dbHost = "127.0.0.1:3306"
    }
    if dbName == "" {
        dbName = "db"
    }
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true", dbUser, dbPass, dbHost, dbName)

    // Use a short context timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Println("db open error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.PingContext(ctx); err != nil {
        log.Println("db ping error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a prepared parameterised query to prevent SQL injection
    var storedHash string
    query := "SELECT password_hash FROM users WHERE username = ?"
    if err := db.QueryRowContext(ctx, query, username).Scan(&storedHash); err != nil {
        if err == sql.ErrNoRows {
            // Do not reveal whether username exists
            http.Error(w, "Unauthorized", http.StatusUnauthorized)
            return
        }
        log.Println("db query error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password hashes using bcrypt (constant-time)
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // Authentication failed
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }

    // Create a secure random session token
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        log.Println("random token error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(b)
    expiresAt := time.Now().Add(24 * time.Hour)

    // Store the session token server-side (example: sessions table)
    // Ensure sessions table exists: sessions(session_token PK, username, expires_at)
    ins := "INSERT INTO sessions (session_token, username, expires_at) VALUES (?, ?, ?)"
    if _, err := db.ExecContext(ctx, ins, sessionToken, username, expiresAt); err != nil {
        log.Println("session insert error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Set a secure, HttpOnly cookie. Secure requires TLS in production.
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        Expires:  expiresAt,
        HttpOnly: true,
        Secure:   true, // ensure HTTPS in production
        SameSite: http.SameSiteStrictMode,
    }
    http.SetCookie(w, cookie)

    // Successful login (generic message to avoid information leakage)
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Logged in"))
}
// ...existing code...
