package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    mysql "github.com/go-sql-driver/mysql"
    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    db            *sql.DB
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_.-]{3,30}$`)
    // Password policy: min length and at least 3 of 4 classes (lower, upper, digit, special).
    minPasswordLen = 12
)

func init() {
    // Load DB connection info from environment — do NOT hardcode credentials.
    // Provide minimal privileges to the DB user (only INSERT/SELECT on users table).
    // Example env vars: DB_USER, DB_PASS, DB_HOST, DB_NAME
    user := os.Getenv("DB_USER")
    pass := os.Getenv("DB_PASS")
    host := os.Getenv("DB_HOST")
    name := os.Getenv("DB_NAME")
    if user == "" || pass == "" || host == "" || name == "" {
        log.Fatal("DB connection environment variables (DB_USER, DB_PASS, DB_HOST, DB_NAME) must be set")
    }

    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=false", user, pass, host, name)
    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }

    // Connection pool / timeouts (defense-in-depth)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)

    // Ping with timeout to fail fast on misconfiguration
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db ping failed: %v", err)
    }
}

func main() {
    http.HandleFunc("/register", registerHandler)

    // In production, run behind TLS (e.g., use a reverse proxy terminating TLS with TLS1.3).
    // For development only:
    log.Println("listening on :8080 (development; use TLS in production)")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce POST and small request size to mitigate abuse
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Limit body size (e.g., 10KB for form data)
    r.Body = http.MaxBytesReader(w, r.Body, 10*1024)

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Input validation: whitelist-based
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (vetted algorithm). Use an appropriate cost (12).
    // Do not use plain SHA or custom salts.
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), 12)
    if err != nil {
        // Do not leak internal errors to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        // Avoid logging sensitive values; log only non-PII error context.
        log.Printf("bcrypt error: %v", err)
        return
    }

    // Store the hash using a prepared statement and context with timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("prepare stmt error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, string(hashed))
    if err != nil {
        // Handle duplicate username without revealing which constraint failed
        var me *mysql.MySQLError
        if errors.As(err, &me) && me.Number == 1062 {
            http.Error(w, "account could not be created", http.StatusConflict)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("db exec error: %v", err)
        return
    }

    // Successful creation
    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("account created"))
}

// validatePassword enforces a basic strong password policy.
// Returns an error with a client-friendly message.
func validatePassword(pw string) error {
    if len(pw) < minPasswordLen {
        return fmt.Errorf("password must be at least %d characters", minPasswordLen)
    }
    var classes int
    for _, ch := range pw {
        switch {
        case 'a' <= ch && ch <= 'z':
            classes |= 1
        case 'A' <= ch && ch <= 'Z':
            classes |= 2
        case '0' <= ch && ch <= '9':
            classes |= 4
        default:
            // Treat everything else as special
            classes |= 8
        }
    }
    // require at least 3 of 4 classes (lower, upper, digit, special)
    cnt := 0
    for i := 0; i < 4; i++ {
        if classes&(1<<i) != 0 {
            cnt++
        }
    }
    if cnt < 3 {
        return fmt.Errorf("password must include multiple character classes (upper, lower, digit, special)")
    }
    return nil
}
