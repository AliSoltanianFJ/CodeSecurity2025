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

func main() {
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("database open error: %v", err)
    }
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(5 * time.Minute)

    if err := db.Ping(); err != nil {
        log.Fatalf("database ping error: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
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
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    if len(username) < 3 || len(username) > 32 {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    var validUsername = regexp.MustCompile(`^[A-Za-z0-9_.-]+$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), 12)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Printf("password hash error: %v", err)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashed)
    if err != nil {
        http.Error(w, "could not create account", http.StatusInternalServerError)
        log.Printf("db insert error: %v", err)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
