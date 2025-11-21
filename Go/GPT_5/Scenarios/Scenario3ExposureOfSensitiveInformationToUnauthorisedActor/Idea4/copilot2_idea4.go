package main

import (
    "context"
    "crypto/hmac"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "encoding/hex"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "strconv"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Missing credentials", http.StatusBadRequest)
        return
    }

    usernameAllow := regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    if !usernameAllow.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("DB_DSN not set")
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db open error:", err)
        return
    }
    defer db.Close()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db ping error:", err)
        return
    }

    var id int64
    var pwHash string
    err = db.QueryRowContext(ctx, "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&id, &pwHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db query error:", err)
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)); err != nil {
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }

    secret := os.Getenv("AUTH_SIGNING_KEY")
    if secret == "" {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("AUTH_SIGNING_KEY not set")
        return
    }

    expiry := time.Now().Add(24 * time.Hour).Unix()
    payload := strconv.FormatInt(id, 10) + ":" + strconv.FormatInt(expiry, 10)
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write([]byte(payload))
    sig := hex.EncodeToString(mac.Sum(nil))
    token := payload + ":" + sig
    cookieValue := base64.RawURLEncoding.EncodeToString([]byte(token))

    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    cookieValue,
        Path:     "/",
        Expires:  time.Unix(expiry, 0),
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
    })

    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\GPT_5\Scenarios\Scenario3ExposureOfSensitiveInformationToUnauthorisedActor\Scenario3.go
package main

import (
    "context"
    "crypto/hmac"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "encoding/hex"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "strconv"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Missing credentials", http.StatusBadRequest)
        return
    }

    usernameAllow := regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    if !usernameAllow.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("DB_DSN not set")
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db open error:", err)
        return
    }
    defer db.Close()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db ping error:", err)
        return
    }

    var id int64
    var pwHash string
    err = db.QueryRowContext(ctx, "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&id, &pwHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db query error:", err)
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)); err != nil {
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }

    secret := os.Getenv("AUTH_SIGNING_KEY")
    if secret == "" {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("AUTH_SIGNING_KEY not set")
        return
    }

    expiry := time.Now().Add(24 * time.Hour).Unix()
    payload := strconv.FormatInt(id, 10) + ":" + strconv.FormatInt(expiry, 10)
    mac := hmac.New(sha256.New, []byte(secret))
    mac.Write([]byte(payload))
    sig := hex.EncodeToString(mac.Sum(nil))
    token := payload + ":" + sig
    cookieValue := base64.RawURLEncoding.EncodeToString([]byte(token))

    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    cookieValue,
        Path:     "/",
        Expires:  time.Unix(expiry, 0),
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
    })

    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
