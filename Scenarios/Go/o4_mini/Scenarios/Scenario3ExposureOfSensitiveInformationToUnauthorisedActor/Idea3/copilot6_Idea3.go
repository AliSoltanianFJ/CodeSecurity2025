package main

import (
    "context"
    "database/sql"
    "errors"
    "log"
    "net/http"
    "sync"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "github.com/google/uuid"
    "golang.org/x/crypto/bcrypt"
)

// sessionStore is a very simple in-memory session storage.
// In production you’d use Redis or your database.
type sessionStore struct {
    mu       sync.RWMutex
    sessions map[string]int64 // map sessionID → userID
}

func newSessionStore() *sessionStore {
    return &sessionStore{
        sessions: make(map[string]int64),
    }
}

func (s *sessionStore) create(userID int64) (string, error) {
    token, err := uuid.NewRandom()
    if err != nil {
        return "", err
    }
    sid := token.String()

    s.mu.Lock()
    s.sessions[sid] = userID
    s.mu.Unlock()

    return sid, nil
}

func (s *sessionStore) getUserID(sid string) (int64, bool) {
    s.mu.RLock()
    uid, ok := s.sessions[sid]
    s.mu.RUnlock()
    return uid, ok
}

var (
    db  *sql.DB
    sessStore = newSessionStore()
)

func main() {
    var err error
    // 1) Open a single DB handle and reuse it for all requests.
    //    The DSN can come from an environment variable or your config.
    db, err = sql.Open(
        "mysql",
        "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4",
    )
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // 2) Configure your connection pool
    db.SetConnMaxLifetime(time.Minute * 5)
    db.SetMaxIdleConns(10)
    db.SetMaxOpenConns(25)

    // 3) Verify that the database is reachable.
    if err := db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit size of the incoming request body to 1MB
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20)

    // parse the form
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // very basic sanity checks; you can tighten these up
    if len(username) < 3 || len(username) > 64 || len(password) < 8 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // use a context with timeout so a slow DB can't hang us forever
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // retrieve the user ID and hashed password from the DB
    var (
        userID       int64
        passwordHash string
    )
    query := `SELECT id, password_hash FROM users WHERE username = ?`
    err := db.QueryRowContext(ctx, query, username).Scan(&userID, &passwordHash)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // do not reveal whether the username or password was wrong
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("DB error in login: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // CompareHashAndPassword uses a constant-time comparison under the hood
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        // wrong password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // At this point the user is authenticated.
    // Create a new session token:
    sessionID, err := sessStore.create(userID)
    if err != nil {
        log.Printf("failed to create session: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Set a secure, HttpOnly cookie
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,                       // inaccessible to JavaScript
        Secure:   true,                       // only over HTTPS in production
        SameSite: http.SameSiteStrictMode,    // or Lax, depending on your needs
        Expires:  time.Now().Add(24 * time.Hour),
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}