package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "fmt"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // parse form values
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use DSN from environment to avoid embedding credentials in source.
    // Example DSN: "user:pass@tcp(localhost:3306)/dbname?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // fallback for local development (avoid using this in production)
        dsn = "root:root@tcp(localhost:3306)/db?parseTime=true"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // short context to avoid long-running queries
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Use prepared statement / parameterized query to avoid SQL injection
    stmt, err := db.PrepareContext(ctx, "SELECT password_hash FROM users WHERE username = ?")
    if err != nil {
        log.Printf("prepare error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var storedHash string
    err = stmt.QueryRowContext(ctx, username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        // don't reveal whether the username or password was wrong
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("query error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Compare the provided password with the stored bcrypt hash
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // On success, create a session token (store it server-side in real app)
    sessionToken, err := generateSessionToken(32)
    if err != nil {
        log.Printf("session token error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Set a secure, HttpOnly cookie. Note: Secure=true requires HTTPS.
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    sessionToken,
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteLaxMode,
        // Expires: time.Now().Add(24 * time.Hour),
    })

    // Minimal success response
    fmt.Fprintln(w, "Login successful")
}

// ...existing code...
// generateSessionToken returns a URL-safe base64 token of n bytes.
func generateSessionToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}
// ...existing code...
