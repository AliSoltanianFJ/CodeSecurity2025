package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"
    "unicode"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var db *sql.DB

func main() {
    // Get DSN from environment to avoid hardcoding credentials.
    dsn := strings.TrimSpace(os.Getenv("DB_DSN"))
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }

    // Configure DB connection pool.
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify connection with timeout.
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("failed to connect to database: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    // Use a real TLS reverse proxy in production. This example listens on :8080 for local testing only.
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Limit request body size to mitigate large payload attacks.
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form with a context deadline.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    // Extract and sanitize inputs.
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate username: allow alphanumeric, dot, underscore, hyphen; length 3-30.
    if !validUsername(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Validate password: length and complexity checks.
    if err := validatePassword(password); err != nil {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt with an appropriate cost.
    const bcryptCost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // Do not leak details about the failure.
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Insert user into DB using a prepared statement and context.
    query := "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)"
    stmtCtx, stmtCancel := context.WithTimeout(ctx, 3*time.Second)
    defer stmtCancel()

    stmt, err := db.PrepareContext(stmtCtx, query)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(stmtCtx, username, hashed, time.Now().UTC())
    if err != nil {
        // Handle duplicate username more gracefully.
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Respond with JSON; do not echo sensitive information.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    _ = json.NewEncoder(w).Encode(map[string]string{"status": "created"})
}

// validUsername validates allowed username characters and length.
func validUsername(u string) bool {
    if len(u) < 3 || len(u) > 30 {
        return false
    }
    // Only allow letters, numbers, dot, underscore, hyphen.
    var usernameRe = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    return usernameRe.MatchString(u)
}

// validatePassword enforces a minimum length and simple complexity rules.
// Do not return complexity specifics to the client; client should be guided separately.
func validatePassword(pw string) error {
    if len(pw) < 12 || len(pw) > 128 {
        return fmt.Errorf("password length")
    }
    var hasUpper, hasLower, hasDigit, hasSymbol bool
    for _, r := range pw {
        switch {
        case unicode.IsUpper(r):
            hasUpper = true
        case unicode.IsLower(r):
            hasLower = true
        case unicode.IsDigit(r):
            hasDigit = true
        case unicode.IsPunct(r) || unicode.IsSymbol(r):
            hasSymbol = true
        }
    }
    // Require at least three of the four classes to balance usability and strength.
    count := 0
    if hasUpper {
        count++
    }
    if hasLower {
        count++
    }
    if hasDigit {
        count++
    }
    if hasSymbol {
        count++
    }
    if count < 3 {
        return fmt.Errorf("password complexity")
    }
    return nil
}

// ...existing code...
