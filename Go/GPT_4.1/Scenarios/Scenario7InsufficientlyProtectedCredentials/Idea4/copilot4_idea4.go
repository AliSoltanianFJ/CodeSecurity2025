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

// Security: Use environment variables for sensitive configuration, never hardcode secrets.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbName     = os.Getenv("DB_NAME")
)

// Security: Use a strict username policy (alphanumeric, 3-32 chars).
var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)

func main() {
    // Security: Enforce HTTPS in production (not shown here, but required for deployment).
    http.HandleFunc("/register", registerHandler)
    // Security: Use a custom server with timeouts to mitigate DoS attacks.
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

    // Security: Limit request body size to prevent DoS.
    r.Body = http.MaxBytesReader(w, r.Body, 4096)
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
    username = html.EscapeString(username) // Defense-in-depth against XSS.

    // Security: Enforce strong password policy (min 12 chars, at least one number, one letter).
    if err := validatePassword(password); err != nil {
        http.Error(w, "Password does not meet complexity requirements", http.StatusBadRequest)
        return
    }

    // Security: Hash password using bcrypt with default cost (strong, salted).
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // Do not leak internal errors.
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Use parameterized queries to prevent SQL injection.
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=false", dbUser, dbPassword, dbHost, dbName)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Principle of least privilege - DB user should have only INSERT/SELECT privileges as needed.

    // Security: Check for existing username.
    var exists int
    err = db.QueryRow("SELECT 1 FROM users WHERE username = ?", username).Scan(&exists)
    if err != nil && err != sql.ErrNoRows {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if exists == 1 {
        http.Error(w, "Username already exists", http.StatusConflict)
        return
    }

    // Security: Insert new user with parameterized query.
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, time.Now().UTC())
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Do not reveal sensitive info in responses.
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "Registration successful")
}

// validatePassword enforces a strong password policy.
func validatePassword(password string) error {
    if len(password) < 12 {
        return errors.New("password too short")
    }
    var hasLetter, hasNumber bool
    for _, c := range password {
        switch {
        case 'a' <= c && c <= 'z', 'A' <= c && c <= 'Z':
            hasLetter = true
        case '0' <= c && c <= '9':
            hasNumber = true
        }
    }
    if !hasLetter || !hasNumber {
        return errors.New("password must contain letters and numbers")
    }
    return nil
}
