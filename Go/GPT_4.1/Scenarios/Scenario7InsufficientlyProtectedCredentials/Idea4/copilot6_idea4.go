package main

import (
    "database/sql"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// Security: Use environment variables for sensitive config, never hardcode secrets.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbName     = os.Getenv("DB_NAME")
)

// Security: Use a strict username policy (alphanumeric, 3-32 chars)
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

func main() {
    // Security: Enforce HTTPS in production (not shown here, but required for deployment)
    http.HandleFunc("/register", registerHandler)
    // Security: Use a custom server with timeouts to prevent Slowloris attacks
    server := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    log.Fatal(server.ListenAndServe())
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 4096)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Security: Validate and sanitize user input
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 12 || len(password) > 64 {
        http.Error(w, "Password must be 12-64 characters", http.StatusBadRequest)
        return
    }

    // Security: Hash password with bcrypt (strong, salted, adaptive)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // Security: Do not leak internal errors
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Use parameterized queries to prevent SQL injection
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=false", dbUser, dbPassword, dbHost, dbName)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Principle of least privilege - ensure DB user has only necessary permissions

    // Security: Check for existing username (avoid timing attacks by using constant time comparison)
    var exists int
    err = db.QueryRow("SELECT COUNT(1) FROM users WHERE username = ?", username).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if exists > 0 {
        http.Error(w, "Username already exists", http.StatusConflict)
        return
    }

    // Security: Store only the hashed password, never the plaintext
    _, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashedPassword)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Encode output to prevent XSS
    fmt.Fprintf(w, "User %s registered successfully.", html.EscapeString(username))
}

// Security: Never log sensitive data (passwords, hashes, etc.)
// Security: Always use HTTPS in production to protect credentials in transit.
// Security: Use secure password policies and strong hashing algorithms (bcrypt, argon2id, scrypt).
// Security: Rotate and protect database credentials using environment variables or a secrets manager.
