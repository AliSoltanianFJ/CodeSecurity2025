package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("missing MYSQL_DSN")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal(err)
    }
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal(err)
    }
    defer db.Close()

    http.HandleFunc("/register", registerHandler)
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    log.Fatal(srv.ListenAndServe())
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "missing parameters", http.StatusBadRequest)
        return
    }

    if len(username) < 3 || len(username) > 30 {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    allowedUser := regexp.MustCompile(`^[A-Za-z0-9_.-]+$`)
    if !allowedUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    if len(password) < 12 || len(password) > 1024 {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), 12)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        log.Printf("db prepare error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed)
    if err != nil {
        log.Printf("db exec error: %v", err)
        if strings.Contains(err.Error(), "Duplicate entry") {
            http.Error(w, "conflict", http.StatusConflict)
            return
        }
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
