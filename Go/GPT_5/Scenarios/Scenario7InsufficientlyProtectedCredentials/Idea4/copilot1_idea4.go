package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "log"
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
        log.Fatal("DB_DSN not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("database open error")
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

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

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    usernameRe := regexp.MustCompile(`^[A-Za-z0-9._-]{3,32}$`)
    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed, time.Now().UTC())
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "registered"})
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\GPT_5\Scenarios\Scenario7InsufficientlyProtectedCredentials\Scenario7.go
package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "log"
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
        log.Fatal("DB_DSN not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("database open error")
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

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

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    usernameRe := regexp.MustCompile(`^[A-Za-z0-9._-]{3,32}$`)
    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed, time.Now().UTC())
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "registered"})
}
