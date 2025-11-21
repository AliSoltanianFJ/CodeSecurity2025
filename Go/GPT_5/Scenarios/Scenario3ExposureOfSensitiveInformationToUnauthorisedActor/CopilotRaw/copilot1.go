package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "net/http"
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
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var passwordHash string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err != nil {
        // Do not reveal whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Compare stored bcrypt hash with supplied password
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Create a random session token and set a secure cookie
    token, err := generateSessionToken()
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set true when using TLS
        Expires:  time.Now().Add(24 * time.Hour),
        SameSite: http.SameSiteLaxMode,
    }
    http.SetCookie(w, cookie)

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Logged in"))
}

// generateSessionToken returns a hex-encoded random token
func generateSessionToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
// ...existing code...
