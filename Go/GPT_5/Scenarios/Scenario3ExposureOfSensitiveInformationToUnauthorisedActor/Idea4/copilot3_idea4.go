// ...existing code...
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

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,64}$`)

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(v)
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 16<<10)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if !usernameRE.MatchString(username) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        http.Error(w, "Service unavailable", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Service unavailable", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1")
    if err != nil {
        http.Error(w, "Service unavailable", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var hash string
    err = stmt.QueryRowContext(ctx, username).Scan(&hash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Service unavailable", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}
