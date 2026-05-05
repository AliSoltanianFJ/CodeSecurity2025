package main

import (
    "context"
    "crypto/sha256"
    "database/sql"
    "fmt"
    "html"
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
    // Initialize DB from environment (do not hardcode credentials)
    dsn := os.Getenv("MYSQL_DSN") // e.g. "user:pass@tcp(127.0.0.1:3306)/dbname?parseTime=true"
    if strings.TrimSpace(dsn) == "" {
        // Fail fast; do not reveal secrets
        fmt.Println("MYSQL_DSN environment variable not set")
        return
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        fmt.Println("database initialization error")
        return
    }
    // Set sensible connection pool limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request size to avoid large payloads
    const maxBodyBytes = 10 * 1024 // 10 KB
    r.Body = http.MaxBytesReader(w, r.Body, maxBodyBytes)

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate username: allow common safe characters, limit length
    var usernameRe = regexp.MustCompile(`^[A-Za-z0-9._-]{3,30}$`)
    if !usernameRe.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Validate password length and basic complexity
    if len(password) < 12 || len(password) > 256 {
        http.Error(w, "Password must be between 12 and 256 characters", http.StatusBadRequest)
        return
    }
    // Basic complexity: require letters and digits (you can strengthen as needed)
    hasLetter := regexp.MustCompile(`[A-Za-z]`).MatchString(password)
    hasDigit := regexp.MustCompile(`\d`).MatchString(password)
    if !hasLetter || !hasDigit {
        http.Error(w, "Password must include letters and numbers", http.StatusBadRequest)
        return
    }

    // Optional: derive/add additional entropy (not as replacement for proper hashing)
    // Example demonstrated: compute a SHA-256 of password+username as extra input to bcrypt.
    // This does NOT replace bcrypt; it's only an additional deterministic mixing step.
    mixed := sha256.Sum256([]byte(username + password))

    // Hash password securely with bcrypt (bcrypt cost 12)
    hashedPassword, err := bcrypt.GenerateFromPassword(append(mixed[:], []byte(password)...), 12)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Prepared statement with parameterized query to prevent SQL injection
    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashedPassword)
    if err != nil {
        // Check for duplicate username (MySQL error 1062)
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Do not echo sensitive information. Provide a generic safe success message.
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("User created"))
    _ = html.EscapeString(username) // kept for completeness if later echoed (avoid XSS)
}
