package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        // fail fast if DSN not provided
        panic("missing MYSQL_DSN")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        panic(err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        panic(err)
    }

    http.HandleFunc("/login", loginHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        panic(err)
    }
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    if username == "" || password == "" {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    if len(username) < 3 || len(username) > 64 || len(password) < 8 || len(password) > 128 {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    var validUser = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var passwordHash string
    err := db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err != nil {
        http.Error(w, "invalid username or password", http.StatusUnauthorized)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "invalid username or password", http.StatusUnauthorized)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
