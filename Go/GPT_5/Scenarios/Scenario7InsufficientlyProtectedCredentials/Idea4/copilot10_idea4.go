package main

import (
    "context"
    "database/sql"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    mysql "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

var usernameRegexp = regexp.MustCompile(`^[A-Za-z0-9_]{3,32}$`)

func main() {
    dsn := os.Getenv("DATABASE_DSN")
    if dsn == "" {
        // fail fast if no DSN provided
        panic("missing DATABASE_DSN")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        panic("database connection failed")
    }
    db.SetConnMaxLifetime(time.Minute * 5)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        panic("database ping failed")
    }

    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    if !usernameRegexp.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashed)
    if err != nil {
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
