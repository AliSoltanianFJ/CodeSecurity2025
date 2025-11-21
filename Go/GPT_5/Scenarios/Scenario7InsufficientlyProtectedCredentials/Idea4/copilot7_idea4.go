package main

// ...existing code...

import (
    "context"
    "database/sql"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    mysqlDriver "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func init() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("missing DB_DSN")
    }
    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatal(err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatal(err)
    }
}

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Missing parameters", http.StatusBadRequest)
        return
    }

    if len(username) < 3 || len(username) > 64 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    var userRE = regexp.MustCompile(`^[A-Za-z0-9._-]{3,64}$`)
    if !userRE.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("hash error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    ctx := r.Context()
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashed)
    if err != nil {
        var me *mysqlDriver.MySQLError
        if errors.As(err, &me) && me.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        log.Printf("db insert error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
}
