// ...existing code...
package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB
var usernameRe = regexp.MustCompile(`^[a-zA-Z0-9_.-]{3,30}$`)

func initDB() {
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        log.Fatal("DATABASE_DSN not set")
    }
    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal(err)
    }
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(time.Minute * 5)
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal(err)
    }
}

func main() {
    initDB()
    http.HandleFunc("/register", registerHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 12 || len(password) > 1024 {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var exists int
    err := db.QueryRowContext(ctx, "SELECT 1 FROM users WHERE username = ? LIMIT 1", username).Scan(&exists)
    if err != nil && err != sql.ErrNoRows {
        log.Printf("db error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if err == nil {
        http.Error(w, "username unavailable", http.StatusConflict)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), 12)
    if err != nil {
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if _, err := db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashed); err != nil {
        log.Printf("db insert error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}