package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // fail fast if no datasource provided
        panic("missing DB_DSN")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        panic("cannot open database")
    }
    // lightweight health check
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        panic("database unreachable")
    }

    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // input validation: allowlist and length checks
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    usrRe := regexp.MustCompile(`^[A-Za-z0-9_-]{3,64}$`)
    if !usrRe.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    if lp := len(password); lp < 8 || lp > 72 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    var hash []byte
    err := db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&hash)
    if err == sql.ErrNoRows {
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    if err := bcrypt.CompareHashAndPassword(hash, []byte(password)); err != nil {
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
}
// ...existing code...
