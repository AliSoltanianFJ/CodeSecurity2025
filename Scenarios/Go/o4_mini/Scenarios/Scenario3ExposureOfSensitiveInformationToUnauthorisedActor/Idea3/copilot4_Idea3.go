package main

import (
    "context"
    "database/sql"
    "errors"
    "log"
    "net/http"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "github.com/gorilla/securecookie"
    "golang.org/x/crypto/bcrypt"
)

var (
    // db is your shared connection pool
    db *sql.DB

    // s is our securecookie instance.  blockKey<>nil => encrypted+authenticated
    s *securecookie.SecureCookie
)

func main() {
    // load your config (DSN, cookie keys) from the environment
    dsn := os.Getenv("DATABASE_DSN") // e.g. "root:root@tcp(localhost:3306)/db?parseTime=true"
    if dsn == "" {
        log.Fatal("DATABASE_DSN is required")
    }

    hashKey := []byte(os.Getenv("HASH_KEY"))   // at least 32 bytes
    blockKey := []byte(os.Getenv("BLOCK_KEY")) // at least 16 bytes
    if len(hashKey) == 0 || len(blockKey) == 0 {
        log.Fatal("HASH_KEY and BLOCK_KEY must be set")
    }
    s = securecookie.New(hashKey, blockKey)

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // optional tuning:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // verify we can talk to MySQL
    if err = db.Ping(); err != nil {
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

    // prevent someone from sending you a 100MB form
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    username := r.Form.Get("username")
    password := r.Form.Get("password")
    if username == "" || password == "" {
        http.Error(w, "Missing username or password", http.StatusBadRequest)
        return
    }

    // give ourselves a deadline on the database
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // fetch the password hash for this user
    var hashFromDB string
    err := db.
        QueryRowContext(ctx,
            "SELECT password_hash FROM users WHERE username = ?",
            username,
        ).
        Scan(&hashFromDB)

    // whether the user didn’t exist or some internal error, we respond the same
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // compare the provided password with the stored hash
    if bcrypt.CompareHashAndPassword([]byte(hashFromDB), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // at this point, username+password are correct.
    // build a small session object
    sessionData := map[string]string{
        "username": username,
    }

    // encode+encrypt+authenticate it
    encoded, err := s.Encode("session", sessionData)
    if err != nil {
        log.Printf("securecookie.Encode: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // set it in a Secure, HTTPOnly cookie
    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    encoded,
        Path:     "/",
        Secure:   true,
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        // you can set Expires/MaxAge here if you want a session timeout
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}