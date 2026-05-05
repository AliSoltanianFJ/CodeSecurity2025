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

// Security: Use environment variables for DB credentials, never hardcode secrets.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbName     = os.Getenv("DB_NAME")
)

// Security: Use a strict username and password policy.
var (
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{4,32}$`)
    passwordRegex = regexp.MustCompile(`^[\S]{12,128}$`) // At least 12 non-whitespace chars
)

func main() {
    // Security: Enforce HTTPS in production (not shown here for brevity).
    http.HandleFunc("/register", registerHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS attacks.
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Security: Validate and sanitize user input.
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !passwordRegex.MatchString(password) {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }
    safeUsername := html.EscapeString(username) // Defense-in-depth for output encoding.

    // Security: Hash password using bcrypt with a strong cost factor.
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("bcrypt error: %v", err)
        return
    }

    // Security: Use least privilege DB user, parameterized queries, and handle errors securely.
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=true", dbUser, dbPassword, dbHost, dbName)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB connection error: %v", err)
        return
    }
    defer db.Close()

    // Security: Set connection timeouts.
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)

    // Security: Check for existing username to prevent duplicates.
    var exists int
    err = db.QueryRow("SELECT COUNT(1) FROM users WHERE username = ?", safeUsername).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }
    if exists > 0 {
        http.Error(w, "Username already exists", http.StatusConflict)
        return
    }

    // Security: Use prepared statements to prevent SQL injection.
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB prepare error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(safeUsername, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB exec error: %v", err)
        return
    }

    // Security: Do not leak sensitive info in responses.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "Registration successful")
}

// Security: Never log or return sensitive data such as passwords or hashes.
// Security: Always use HTTPS in production to protect credentials in transit.
// Security: Rotate and restrict DB credentials, and use least privilege for DB user.
