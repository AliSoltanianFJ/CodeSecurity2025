package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "log"
    "net/http"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    var err error

    // 1) Open your DB once and configure pooling
    //    use parseTime=true if you have DATETIME/TIMESTAMP columns
    dsn := "root:root@tcp(localhost:3306)/db?parseTime=true"
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // optional but recommended tuning
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // verify that we can talk to the database
    if err = db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler handles a POST /login with form values "username" and "password".
// On success it issues a secure session cookie.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Grab form values
    username := r.FormValue("username")
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // 2) Look up the hashed password for this user
    var userID int64
    var passwordHash []byte
    err := db.QueryRow(
        "SELECT id, password_hash FROM users WHERE username = ?",
        username,
    ).Scan(&userID, &passwordHash)
    if err != nil {
        // Don't reveal whether it's the username or password that failed
        if err == sql.ErrNoRows {
            http.Error(w, "invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db.QueryRow: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // 3) Compare the stored hash with the supplied password
    if err := bcrypt.CompareHashAndPassword(passwordHash, []byte(password)); err != nil {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    // 4) Generate a new session token
    token, err := generateSessionToken()
    if err != nil {
        log.Printf("generateSessionToken: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }
    expiresAt := time.Now().Add(24 * time.Hour)

    // 5) Store that token in your session store (here: a simple sessions table)
    //    You can also use Redis, an in-memory map with expirations, JWTs, etc.
    _, err = db.Exec(`
        INSERT INTO sessions (user_id, token, expires_at)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE
          token = VALUES(token),
          expires_at = VALUES(expires_at)
    `, userID, token, expiresAt)
    if err != nil {
        log.Printf("db.Exec(sessions): %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // 6) Send the cookie back to the user
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        Expires:  expiresAt,
        HttpOnly: true,
        Secure:   true,                          // only send over HTTPS!
        SameSite: http.SameSiteStrictMode,       // or LaxMode if you need cross-site POSTs
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("login successful"))
}

// generateSessionToken returns a URL-safe, base64-encoded random string
func generateSessionToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.URLEncoding.EncodeToString(b), nil
}