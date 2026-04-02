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
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

// Session holds a logged‐in user’s info.
// In production you’d use Redis, Memcache, a database, etc.
type Session struct {
    UserID int
    Expiry time.Time
}

var (
    db            *sql.DB
    sessions      = make(map[string]Session)
    sessionsMutex sync.Mutex
)

func main() {
    // Read DSN from an environment variable
    // e.g. export DB_DSN="appuser:secret@tcp(db.mycompany.internal:3306)/appdb?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Configure the connection pool
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify we can talk to the database
    if err := db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // ParseForm is a no-op if content-type is application/x-www-form-urlencoded,
    // and FormValue will do it automatically, but it’s good to be explicit
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // very basic length checks
    if len(username) < 3 || len(username) > 64 || len(password) < 8 {
        // note: do NOT reveal which one was wrong
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // use a Context with timeout so a slow DB can’t hang us forever
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // fetch the stored password hash and the user’s ID
    const query = `
        SELECT id, password_hash
        FROM users
        WHERE username = ?
        LIMIT 1
    `
    var (
        userID       int
        passwordHash string
    )
    err := db.QueryRowContext(ctx, query, username).Scan(&userID, &passwordHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("db.QueryRowContext: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare the password the user sent with the bcrypt‐hashed password stored in the database
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        // wrong password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // At this point the credentials are valid. Generate a new session token:
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        log.Printf("rand.Read: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(tokenBytes)
    expiry := time.Now().Add(24 * time.Hour)

    // Store it in our in‐memory session map (demo only! use a real store in production)
    sessionsMutex.Lock()
    sessions[sessionToken] = Session{
        UserID: userID,
        Expiry: expiry,
    }
    sessionsMutex.Unlock()

    // Set the secure cookie
    cookie := &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        Path:     "/",
        Expires:  expiry,
        HttpOnly: true,
        Secure:   true, // require HTTPS in production
        SameSite: http.SameSiteLaxMode,
    }
    http.SetCookie(w, cookie)

    w.WriteHeader(http.StatusOK)
    fmt.Fprintln(w, "Login successful")
}