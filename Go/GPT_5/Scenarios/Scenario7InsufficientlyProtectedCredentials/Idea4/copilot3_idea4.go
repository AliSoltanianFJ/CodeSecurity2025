package main

import (
    "context"
    "database/sql"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        panic("missing DB_DSN")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        panic(err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        panic(err)
    }

    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    if username == "" || password == "" {
        http.Error(w, "missing fields", http.StatusBadRequest)
        return
    }

    if len(username) < 3 || len(username) > 30 {
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }
    if len(password) < 12 || len(password) > 1024 {
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }

    var validUser = regexp.MustCompile(`^[A-Za-z0-9_]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid input", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())")
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    if _, err := stmt.ExecContext(ctx, username, string(hashed)); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
