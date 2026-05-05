// ...existing code...
package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    // open DB once and reuse
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // fallback; avoid hardcoding real secrets in production
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }
    // basic connection tuning
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // verify connectivity
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db ping failed: %v", err)
    }

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit request body size to mitigate certain attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // basic validation
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if len(username) > 256 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // query the hashed password using a parameterized query
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    const query = "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    var passwordHash string
    err := db.QueryRowContext(ctx, query, username).Scan(&passwordHash)
    if err != nil {
        // don't reveal whether username exists
        if err == sql.ErrNoRows {
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // compare bcrypt hash
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        // wrong password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // successful login (in production set a secure session cookie, not plain text)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("Login successful"))
}
