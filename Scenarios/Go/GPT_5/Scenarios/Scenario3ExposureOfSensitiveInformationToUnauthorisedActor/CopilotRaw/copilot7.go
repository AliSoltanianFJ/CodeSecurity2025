package main

import (
    "context"
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "log"
    "net/http"
    "os"
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

// ...existing code...

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        // Do not reveal which field is missing
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Prefer DSN from environment for real deployments; fallback to local default.
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Println("db open error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a short context timeout for the DB request
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var pwHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&pwHash)
    if err == sql.ErrNoRows {
        // Avoid revealing whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Println("db query error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password using bcrypt
    if bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a random session token (store server-side in a real app)
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Println("token generation error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(tokenBytes)

    // Set secure cookie attributes; note: Secure should be true when using TLS.
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
        // Secure: true, // enable in production with HTTPS
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Logged in"))
}
