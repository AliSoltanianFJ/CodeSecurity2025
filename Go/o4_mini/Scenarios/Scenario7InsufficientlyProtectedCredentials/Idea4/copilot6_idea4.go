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

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...

var db *sql.DB

func main() {
    // Read DSN from environment - do NOT hardcode credentials in source.
    dsn := os.Getenv("DB_DSN") // example: "user:password@tcp(localhost:3306)/dbname?parseTime=true"
    if dsn == "" {
        log.Fatal("DB_DSN environment variable not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }
    // Connection pool hardening
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // Verify connection early
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db ping failed: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    // Recommend running behind a reverse proxy terminating TLS. This example listens on :8080.
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Default secure headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // HSTS only if request is TLS (set by proxy in production)
    if r.TLS != nil {
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }

    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce TLS (if you are behind a TLS-terminating proxy, validate via headers there)
    if r.TLS == nil {
        http.Error(w, "use HTTPS when submitting credentials", http.StatusUpgradeRequired)
        return
    }

    // Parse form with small default memory allowance
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Input validation (whitelist)
    if err := validateUsername(username); err != nil {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, "password does not meet complexity requirements", http.StatusBadRequest)
        return
    }

    // Hash the password with bcrypt (vetted algorithm). Use cost 12 for reasonable security; adjust per your environment.
    const bcryptCost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Store user safely with parameterized query and a context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    query := "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)"
    _, err = db.ExecContext(ctx, query, username, string(hashed), time.Now().UTC())
    if err != nil {
        // handle duplicate username (MySQL error 1062)
        if isMySQLDuplicateEntry(err) {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Respond with minimal information
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "account created")
}

// validateUsername enforces a strict whitelist: 3-32 chars, letters, digits, dot, underscore, hyphen.
func validateUsername(u string) error {
    if u == "" {
        return errors.New("empty username")
    }
    if len(u) < 3 || len(u) > 32 {
        return errors.New("username length")
    }
    var usernameRE = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !usernameRE.MatchString(u) {
        return errors.New("invalid characters")
    }
    return nil
}

// validatePassword enforces minimal complexity: length and character classes.
// Adjust policy according to threat model (consider zxcvbn and banned-password lists in production).
func validatePassword(p string) error {
    if len(p) < 12 {
        return errors.New("password too short")
    }
    var hasLower = regexp.MustCompile(`[a-z]`).MatchString
    var hasUpper = regexp.MustCompile(`[A-Z]`).MatchString
    var hasDigit = regexp.MustCompile(`[0-9]`).MatchString
    var hasSymbol = regexp.MustCompile(`[\W_]`).MatchString

    if !hasLower(p) || !hasUpper(p) || !hasDigit(p) || !hasSymbol(p) {
        return errors.New("password complexity")
    }
    return nil
}

// isMySQLDuplicateEntry detects duplicate entry error from MySQL driver.
// Keeps logic minimal; extend to inspect driver-specific error types if needed.
func isMySQLDuplicateEntry(err error) bool {
    if err == nil {
        return false
    }
    // MySQL duplicate entry message contains "Duplicate entry"
    return regexp.MustCompile(`(?i)duplicate entry`).MatchString(err.Error())
}

// ...existing code...
