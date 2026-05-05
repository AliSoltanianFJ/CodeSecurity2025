package main

import (
    "context"
    "database/sql"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
var db *sql.DB

func main() {
    // initialize DB from environment (do not hardcode credentials)
    dsn := os.Getenv("DB_DSN") // e.g. "user:pass@tcp(localhost:3306)/dbname?parseTime=true"
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("database open error: %v", err)
    }
    // verify connection
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("database ping error: %v", err)
    }
    // reasonable connection pool limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    http.HandleFunc("/register", registerHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit request size to prevent abuse
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB

    // parse form
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // get and normalize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // basic validation and sanitisation
    // username: 3-30 chars, letters, numbers, underscore, dot, dash
    usernameRegex := regexp.MustCompile(`^[A-Za-z0-9_.-]{3,30}$`)
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // password length bounds to avoid DoS via extreme bcrypt costs
    if len(password) < 12 || len(password) > 128 {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }
    // simple complexity check (must contain letter and number)
    hasLetter := false
    hasDigit := false
    for _, c := range password {
        switch {
        case c >= '0' && c <= '9':
            hasDigit = true
        case (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'):
            hasLetter = true
        }
    }
    if !hasLetter || !hasDigit {
        http.Error(w, "Password must contain letters and numbers", http.StatusBadRequest)
        return
    }

    // hash password with bcrypt (salted, adaptive)
    const bcryptCost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // store user using parameterized query to prevent SQL injection
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashed))
    if err != nil {
        // detect duplicate username (MySQL error 1062) without leaking details
        var me *mysql.MySQLError
        if errors.As(err, &me) && me.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // do not echo back any sensitive or user-provided content
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("User created"))
}
