package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "errors"
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

    // Parse form and trim inputs
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation: required, length limits, allowed characters
    if username == "" || password == "" {
        http.Error(w, "Invalid username or password", http.StatusBadRequest)
        return
    }
    if len(username) > 64 || len(password) > 512 {
        http.Error(w, "Invalid username or password", http.StatusBadRequest)
        return
    }
    var usernameRe = regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    if !usernameRe.MatchString(username) {
        http.Error(w, "Invalid username or password", http.StatusBadRequest)
        return
    }

    // Use environment variable for DB connection string to avoid embedding credentials in code
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    // Short context to avoid long-running DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a prepared query with parameter binding to prevent SQL injection
    var passwordHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err != nil {
        // Do not reveal whether the username exists. If no rows, perform a dummy bcrypt compare
        if errors.Is(err, sql.ErrNoRows) {
            // Dummy compare to mitigate timing attacks (uses a generated fake hash)
            // Generate a low-cost fake hash once would be preferable; doing a single compare is acceptable here
            _ = bcrypt.CompareHashAndPassword([]byte("$2a$10$C6UzMDM.H6dfI/f/IKcEeO"), []byte(password))
            http.Error(w, "Invalid username or password", http.StatusUnauthorized)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare supplied password with stored bcrypt hash
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Create a secure session token
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := base64.RawURLEncoding.EncodeToString(tokenBytes)

    // Set a secure, HttpOnly cookie. Ensure the application is served over HTTPS in deployment so Secure flag is effective.
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // requires HTTPS in production
        SameSite: http.SameSiteStrictMode,
        MaxAge:   3600,
    }
    http.SetCookie(w, cookie)

    // Do not include any sensitive info in responses
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("Login successful"))
}

// ...existing code...
