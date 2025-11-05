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

// Security: Use a strict username policy (alphanumeric, 3-32 chars).
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

func main() {
    // Security: Enforce HTTPS in production (not shown here for brevity).
    http.HandleFunc("/register", registerHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// registerHandler handles user registration securely.
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS attacks.
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    // Parse form data securely.
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Security: Validate and sanitize username.
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    username = html.EscapeString(username) // Defense-in-depth for output encoding.

    // Security: Enforce strong password policy (min 12 chars, at least 1 number, 1 letter).
    if err := validatePassword(password); err != nil {
        http.Error(w, "Weak password: "+err.Error(), http.StatusBadRequest)
        return
    }

    // Security: Hash password with bcrypt (industry standard, includes salt).
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("bcrypt error: %v", err)
        return
    }

    // Security: Use parameterized queries to prevent SQL injection.
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=false", dbUser, dbPassword, dbHost, dbName)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB open error: %v", err)
        return
    }
    defer db.Close()

    // Security: Principle of least privilege—ensure DB user has only necessary permissions.

    // Security: Check if username already exists.
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", username).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }
    if exists {
        http.Error(w, "Username already taken", http.StatusConflict)
        return
    }

    // Security: Store only hashed password, never plaintext.
    _, err = db.Exec(
        "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)",
        username, hashedPassword, time.Now().UTC(),
    )
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB insert error: %v", err)
        return
    }

    // Security: Do not leak sensitive info in responses.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "Registration successful")
}

// validatePassword enforces a strong password policy.
func validatePassword(pw string) error {
    if len(pw) < 12 {
        return errors.New("must be at least 12 characters")
    }
    hasLetter := regexp.MustCompile(`[A-Za-z]`).MatchString(pw)
    hasNumber := regexp.MustCompile(`[0-9]`).MatchString(pw)
    if !hasLetter || !hasNumber {
        return errors.New("must contain both letters and numbers")
    }
    return nil
}
