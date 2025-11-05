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

// Security: Use a strict username policy (alphanumeric, 3-32 chars).
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

// Security: Minimum password policy (at least 12 chars, at least one number, one letter).
var passwordRegex = regexp.MustCompile(`^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]{12,}$`)

func main() {
    // Security: Always use HTTPS in production.
    http.HandleFunc("/register", registerHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// registerHandler handles user registration securely.
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS.
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    // Parse form data securely.
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Security: Validate and sanitize username and password.
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !passwordRegex.MatchString(password) {
        http.Error(w, "Password does not meet complexity requirements", http.StatusBadRequest)
        return
    }
    safeUsername := html.EscapeString(username) // Defense-in-depth for output encoding.

    // Security: Hash password with bcrypt (strong, salted).
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("bcrypt error: %v", err)
        return
    }

    // Security: Use least privilege DB user, parameterized queries, and short connection lifetime.
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=false", dbUser, dbPassword, dbHost, dbName)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB open error: %v", err)
        return
    }
    defer db.Close()
    db.SetConnMaxLifetime(2 * time.Minute)
    db.SetMaxOpenConns(2)
    db.SetMaxIdleConns(1)

    // Security: Check for existing username (avoid timing attacks by always hashing).
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", username).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }
    if exists {
        http.Error(w, "Username already exists", http.StatusConflict)
        return
    }

    // Security: Use prepared statement to prevent SQL injection.
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB prepare error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB exec error: %v", err)
        return
    }

    // Security: Do not leak sensitive info in responses.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "User %s registered successfully.", safeUsername)
}
