package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "fmt"
    "log"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use environment variable for DSN when available (safer than hard-coding)
    dsn := os.Getenv("DB_DSN")
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

    if err := db.Ping(); err != nil {
        log.Println("db ping error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use prepared statement / parameterized query to avoid SQL injection
    var storedHash string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Println("db query error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password with bcrypt hashed password stored in DB
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Create a simple session token (store server-side in real apps)
    token, err := generateSessionToken(32)
    if err != nil {
        log.Println("token generation error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Set secure cookie flags where appropriate. Secure=true should be used in production with HTTPS.
    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    token,
        HttpOnly: true,
        Secure:   false, // set to true in production (HTTPS)
        Path:     "/",
        SameSite: http.SameSiteLaxMode,
    })

    w.WriteHeader(http.StatusOK)
    fmt.Fprintln(w, "Login successful")
}

func generateSessionToken(n int) (string, error) {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}