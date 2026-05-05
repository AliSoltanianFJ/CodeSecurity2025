package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "fmt"
    "log"
    "net/http"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var passwordHash string
    err = db.QueryRowContext(r.Context(), "SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Println("db query:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Create a session token (persist server-side in a real app)
    token, err := generateSessionToken(32)
    if err != nil {
        log.Println("token:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set to true when using HTTPS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    fmt.Fprintf(w, "Welcome, %s! Login successful.", username)
}

func generateSessionToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
```// filepath:
