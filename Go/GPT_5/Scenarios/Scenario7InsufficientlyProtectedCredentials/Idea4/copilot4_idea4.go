package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("missing DB_DSN environment variable")
    }
    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal("database open error")
    }
    db.SetConnMaxLifetime(time.Minute * 5)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    http.HandleFunc("/register", registerHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// ...existing code...
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
        http.Error(w, "missing parameters", http.StatusBadRequest)
        return
    }

    if len(username) < 3 || len(username) > 32 {
        http.Error(w, "invalid parameters", http.StatusBadRequest)
        return
    }
    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "invalid parameters", http.StatusBadRequest)
        return
    }

    var validUsername = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "invalid parameters", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    stmt := "INSERT INTO users (username, password_hash) VALUES (?, ?)"
    _, err = db.ExecContext(ctx, stmt, username, string(hashed))
    if err != nil {
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "user exists", http.StatusConflict)
            return
        }
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
