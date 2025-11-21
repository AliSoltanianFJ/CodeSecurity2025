package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    // DSN should come from environment for security. Example:
    // export DB_DSN="user:pass@tcp(localhost:3306)/dbname?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }

    // Set reasonable limits for connection pooling
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify DB connectivity with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("failed to ping database: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    // NOTE: Serve behind TLS in production (e.g., use a reverse proxy that terminates TLS).
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

type jsonResp struct {
    Message string `json:"message"`
}

// registerHandler handles user registration securely.
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Always reply JSON and avoid reflecting raw input back to client (mitigates XSS)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    if r.Method != http.MethodPost {
        http.Error(w, `{"message":"method not allowed"}`, http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to avoid resource exhaustion / large uploads
    r.Body = http.MaxBytesReader(w, r.Body, 10*1024) // 10 KB should be plenty for credentials
    if err := r.ParseForm(); err != nil {
        http.Error(w, `{"message":"invalid form data"}`, http.StatusBadRequest)
        return
    }

    // Extract and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, `{"message":"username and password are required"}`, http.StatusBadRequest)
        return
    }

    // Basic username validation: length and allowed characters
    if err := validateUsername(username); err != nil {
        http.Error(w, fmt.Sprintf(`{"message":"%s"}`, html.EscapeString(err.Error())), http.StatusBadRequest)
        return
    }

    // Basic password policy
    if err := validatePassword(password); err != nil {
        http.Error(w, fmt.Sprintf(`{"message":"%s"}`, html.EscapeString(err.Error())), http.StatusBadRequest)
        return
    }

    // Hash password with bcrypt (cost 12). Do NOT use plain SHA variants for passwords.
    const bcryptCost = 12
    pwHash, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // Do not reveal internal details
        log.Printf("password hashing error: %v", err)
        http.Error(w, `{"message":"internal error"}`, http.StatusInternalServerError)
        return
    }

    // Use context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Check if username already exists using prepared statement to avoid SQL injection
    var exists int
    err = db.QueryRowContext(ctx, "SELECT 1 FROM users WHERE username = ? LIMIT 1", username).Scan(&exists)
    if err != nil && err != sql.ErrNoRows {
        log.Printf("db query error: %v", err)
        http.Error(w, `{"message":"internal error"}`, http.StatusInternalServerError)
        return
    }
    if exists == 1 {
        http.Error(w, `{"message":"username already exists"}`, http.StatusConflict)
        return
    }

    // Insert new user (no sensitive info returned). Use ExecContext and placeholders.
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)", username, pwHash, time.Now().UTC())
    if err != nil {
        // Handle duplicate insert race condition gracefully
        if isMySQLDuplicateEntry(err) {
            http.Error(w, `{"message":"username already exists"}`, http.StatusConflict)
            return
        }
        log.Printf("db insert error: %v", err)
        http.Error(w, `{"message":"internal error"}`, http.StatusInternalServerError)
        return
    }

    // Success - do not include the username or sensitive info in the response body
    resp := jsonResp{Message: "registration successful"}
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        log.Printf("response encode error: %v", err)
    }
}

// validateUsername enforces a conservative username policy.
func validateUsername(u string) error {
    if len(u) < 3 || len(u) > 30 {
        return errors.New("username must be between 3 and 30 characters")
    }
    // Allow letters, numbers, dot, underscore, hyphen
    var valid = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !valid.MatchString(u) {
        return errors.New("username contains invalid characters")
    }
    return nil
}

// validatePassword enforces a conservative password policy.
func validatePassword(p string) error {
    // Minimum length; recommend passphrases (longer is better)
    if len(p) < 12 {
        return errors.New("password must be at least 12 characters")
    }
    // Optionally enforce more checks (uppercase, number, symbol) depending on policy.
    return nil
}

// isMySQLDuplicateEntry detects MySQL duplicate key error codes.
// This is a simple helper—adjust to match your driver/DB error messages if needed.
func isMySQLDuplicateEntry(err error) bool {
    if err == nil {
        return false
    }
    // The mysql driver returns *mysql.MySQLError; avoid importing it directly to keep code generic.
    // Fallback to substring check (conservative). Do not reveal DB internals to clients.
    return strings.Contains(strings.ToLower(err.Error()), "duplicate") || strings.Contains(strings.ToLower(err.Error()), "unique")
}
