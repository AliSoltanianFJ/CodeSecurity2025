package main

import (
    "context"
    "database/sql"
    "errors"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    // DSN should be provided via environment variable to avoid hardcoding credentials.
    // Example: export MYSQL_DSN="user:password@tcp(127.0.0.1:3306)/dbname?parseTime=true"
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("database open error: %v", err)
    }
    // connection pool tuning
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // verify connection
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("database ping error: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to mitigate DoS / large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB

    // Accept only expected content types
    ct := r.Header.Get("Content-Type")
    if !strings.HasPrefix(ct, "application/x-www-form-urlencoded") && !strings.HasPrefix(ct, "multipart/form-data") {
        http.Error(w, "Unsupported content type", http.StatusBadRequest)
        return
    }

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    // Extract and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate inputs
    if err := validateUsername(username); err != nil {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, "Password does not meet complexity requirements", http.StatusBadRequest)
        return
    }

    // Hash password with bcrypt (adaptive, salted)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // avoid leaking internal errors or secrets
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Store user record using a prepared statement and context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, string(hashed), time.Now().UTC())
    if err != nil {
        // handle duplicate username gracefully without exposing DB details
        var mysqlErr *mysql.MySQLError
        if errors.As(err, &mysqlErr) && mysqlErr.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Do not reflect raw user input back to the client. If any value must be shown, escape it.
    safeUsername := html.EscapeString(username)

    w.WriteHeader(http.StatusCreated)
    _, _ = w.Write([]byte("User created"))
    _ = safeUsername // placeholder if you need to log safely; do not log secrets like passwords
}

// validateUsername enforces a tight whitelist: 3-30 chars, letters, numbers, underscore, hyphen.
func validateUsername(u string) error {
    if len(u) < 3 || len(u) > 30 {
        return errors.New("username length")
    }
    // only allow specific characters
    validUsername := regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !validUsername.MatchString(u) {
        return errors.New("invalid characters")
    }
    return nil
}

// validatePassword enforces minimum length and complexity.
// Adjust rules to your policy. Avoid returning specifics that help attackers.
func validatePassword(p string) error {
    if len(p) < 12 {
        return errors.New("too short")
    }
    var (
        upper = regexp.MustCompile(`[A-Z]`)
        lower = regexp.MustCompile(`[a-z]`)
        digit = regexp.MustCompile(`[0-9]`)
        sym   = regexp.MustCompile(`[\W_]`)
    )
    if !upper.MatchString(p) || !lower.MatchString(p) || !digit.MatchString(p) || !sym.MatchString(p) {
        return errors.New("insufficient complexity")
    }
    return nil
}
