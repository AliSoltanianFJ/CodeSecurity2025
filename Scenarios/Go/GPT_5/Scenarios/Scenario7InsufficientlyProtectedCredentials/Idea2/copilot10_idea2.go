package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    mysqlDriver "github.com/go-sql-driver/mysql"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func init() {
    dsn := strings.TrimSpace(os.Getenv("DB_DSN"))
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required (format: user:pass@tcp(host:port)/dbname?parseTime=true)")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("database open failed: %v", err)
    }

    // set sensible connection pool limits
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("database ping failed: %v", err)
    }
}

func main() {
    http.HandleFunc("/register", registerHandler)

    // Prefer TLS; require certificate and key to be provided via env vars.
    cert := strings.TrimSpace(os.Getenv("TLS_CERT_FILE"))
    key := strings.TrimSpace(os.Getenv("TLS_KEY_FILE"))
    addr := ":8080"
    if cert != "" && key != "" {
        log.Printf("Starting HTTPS server on %s", addr)
        log.Fatal(http.ListenAndServeTLS(addr, cert, key, nil))
    }

    log.Fatal("TLS_CERT_FILE and TLS_KEY_FILE environment variables are not set; refusing to start without TLS")
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers to reduce XSS/Clickjacking risk
    w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent abuse
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // Read and sanitize input
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate input presence
    if username == "" || password == "" {
        http.Error(w, "username and password are required", http.StatusBadRequest)
        return
    }

    // Validate username: only allow safe characters and length limits
    if len(username) < 3 || len(username) > 30 {
        http.Error(w, "username must be between 3 and 30 characters", http.StatusBadRequest)
        return
    }
    usernameRE := regexp.MustCompile(`^[A-Za-z0-9_]+$`)
    if !usernameRE.MatchString(username) {
        http.Error(w, "username contains invalid characters", http.StatusBadRequest)
        return
    }

    // Validate password strength
    if len(password) < 12 {
        http.Error(w, "password must be at least 12 characters", http.StatusBadRequest)
        return
    }
    // Require at least one digit, one lower, one upper, one special
    hasLower := regexp.MustCompile(`[a-z]`).MatchString(password)
    hasUpper := regexp.MustCompile(`[A-Z]`).MatchString(password)
    hasDigit := regexp.MustCompile(`\d`).MatchString(password)
    hasSpecial := regexp.MustCompile(`[^\w\s]`).MatchString(password)
    if !(hasLower && hasUpper && hasDigit && hasSpecial) {
        http.Error(w, "password must include upper, lower, digit and special character", http.StatusBadRequest)
        return
    }

    // Hash password with bcrypt (best practice for password storage)
    hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // Do not leak details to client; log safely
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Store in DB using a context with timeout and prepared statement to avoid SQL injection
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, `INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?);`)
    if err != nil {
        log.Printf("prepare statement error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, string(hash), time.Now().UTC())
    if err != nil {
        // Detect duplicate username for a nicer message (MySQL duplicate entry = 1062)
        var mysqlErr *mysqlDriver.MySQLError
        if errors.As(err, &mysqlErr) && mysqlErr.Number == 1062 {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }

        log.Printf("db insert error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Send minimal safe response; escape any user data if reflected
    safeUser := html.EscapeString(username)
    fmt.Fprintf(w, "user %s registered", safeUser)
}
