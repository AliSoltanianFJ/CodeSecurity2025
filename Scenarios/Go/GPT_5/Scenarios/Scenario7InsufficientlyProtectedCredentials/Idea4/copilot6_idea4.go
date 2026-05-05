package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_-]{3,30}$`)

func main() {
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        log.Fatal("missing DATABASE_DSN")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("db open failed")
    }
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(5 * time.Minute)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal("db ping failed")
    }

    http.HandleFunc("/register", registerHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if !usernameRE.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 12 || len(password) > 256 {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    var exists int
    err := db.QueryRowContext(ctx, "SELECT 1 FROM users WHERE username = ? LIMIT 1", username).Scan(&exists)
    if err == nil {
        http.Error(w, "username already taken", http.StatusConflict)
        return
    }
    if err != nil && err != sql.ErrNoRows {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashed))
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
