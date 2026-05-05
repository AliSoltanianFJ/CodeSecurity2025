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
    "strings"
    "time"
    "unicode/utf8"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var db *sql.DB

func main() {
    // initialize DB from environment variable to avoid embedding credentials in source
    dsn := strings.TrimSpace(os.Getenv("MYSQL_DSN"))
    if dsn == "" {
        log.Fatal("environment variable MYSQL_DSN is required (format: user:pass@tcp(host:port)/dbname?parseTime=true)")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }

    // reasonable pool limits
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)

    // verify DB connectivity with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("failed to ping db: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    // NOTE: Deploy behind TLS (reverse proxy or use ListenAndServeTLS in production)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("http server error: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // limit request size to mitigate large payload attacks
    r.Body = http.MaxBytesReader(w, r.Body, 16<<10) // 16KB

    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // parse form with a small memory limit
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // input validation rules
    if err := validateUsername(username); err != nil {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, "invalid password", http.StatusBadRequest)
        return
    }

    // hash the password with bcrypt (secure, adaptive)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // do not reveal internal error details to the client
        log.Printf("password hashing error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // store user securely using parameterized query and context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())")
    if err != nil {
        log.Printf("db prepare error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, string(hashed))
    if err != nil {
        // detect duplicate username (MySQL error code 1062) in a safe way
        if isDuplicateEntryError(err) {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        log.Printf("db exec error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // success: do not echo sensitive information back
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "account created")
}

// validateUsername enforces a tight allowed set to reduce injection/XSS risk
func validateUsername(u string) error {
    if u == "" {
        return errors.New("empty")
    }
    // allow letters, digits and a few characters, length 3-30
    if utf8.RuneCountInString(u) < 3 || utf8.RuneCountInString(u) > 30 {
        return errors.New("invalid length")
    }
    re := regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !re.MatchString(u) {
        return errors.New("invalid chars")
    }
    return nil
}

// validatePassword enforces minimum entropy/length requirements
func validatePassword(p string) error {
    if p == "" {
        return errors.New("empty")
    }
    // require between 8 and 128 characters (adjust policy as needed)
    if l := utf8.RuneCountInString(p); l < 8 || l > 128 {
        return errors.New("invalid length")
    }
    return nil
}

// isDuplicateEntryError attempts to detect MySQL duplicate key error without exposing internals
func isDuplicateEntryError(err error) bool {
    // driver-specific errors may vary; detect common markers safely
    errStr := strings.ToLower(err.Error())
    return strings.Contains(errStr, "duplicate") || strings.Contains(errStr, "error 1062")
}
