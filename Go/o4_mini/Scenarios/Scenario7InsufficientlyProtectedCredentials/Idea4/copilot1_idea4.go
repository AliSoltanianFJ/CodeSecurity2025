package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
    "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // DB DSN must be provided via environment variable in production.
    // Example DSN (do not hardcode in source): DB_DSN="user:password@tcp(127.0.0.1:3306)/dbname?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }
    // Connection pool and timeouts - enforce least privilege on DB user (create user with minimal rights)
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // Verify DB connectivity with a timeout
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db ping failed: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    // In production, run behind TLS (nginx/ALB) or use ListenAndServeTLS with proper certs.
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only accept POST
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Ensure form parsing (limit memory)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation: strict whitelist for username and password policy
    if !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !isValidPassword(password) {
        http.Error(w, "Password does not meet complexity requirements (min 12 chars, mixed case, digit, symbol)", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (no custom hashing)
    const bcryptCost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // Do not reveal internal errors to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Store in DB using parameterized prepared statement and context with timeout
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    if _, err := stmt.ExecContext(ctx, username, hashed); err != nil {
        // Handle duplicate username gracefully without leaking DB details
        if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Successful creation
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user created")
}

// isValidUsername enforces a whitelist: 3-30 chars, letters, digits, ., _, -
func isValidUsername(u string) bool {
    if u == "" {
        return false
    }
    var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,30}$`)
    return usernameRE.MatchString(u)
}

// isValidPassword enforces a minimum length and basic complexity checks.
// Consider using a password strength library or blocklist in production.
func isValidPassword(p string) bool {
    if len(p) < 12 {
        return false
    }
    var (
        upper = regexp.MustCompile(`[A-Z]`)
        lower = regexp.MustCompile(`[a-z]`)
        digit = regexp.MustCompile(`[0-9]`)
        sym   = regexp.MustCompile(`[\W_]`) // non-word or underscore as symbol
    )
    return upper.MatchString(p) && lower.MatchString(p) && digit.MatchString(p) && sym.MatchString(p)
}
