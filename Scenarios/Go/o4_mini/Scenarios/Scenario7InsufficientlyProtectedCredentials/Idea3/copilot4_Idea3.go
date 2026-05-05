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

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    // db is our global database handle
    db *sql.DB

    // ErrUserExists is returned when a registration is attempted
    // with a username that already exists.
    ErrUserExists = errors.New("username already taken")

    // config parameters
    maxUsernameLen = 50
    minPasswordLen = 8
    maxPasswordLen = 72 // bcrypt limitation
    // allow letters, digits, underscores; adjust to your policy
    validUsername = regexp.MustCompile(`^[A-Za-z0-9_]+$`)
)

func main() {
    // 1) Read DSN (user:pass@tcp(host:port)/dbname?parseTime=true) from env
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // Set reasonable limits on connection pool
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify the database is reachable.
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Printf("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// registerHandler handles POST /register
// It expects form values "username" and "password".
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 1) Parse & validate input
    if err := r.ParseForm(); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if err := validateCredentials(username, password); err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }

    // 2) Hash the password with bcrypt
    // Note: bcrypt has a maximum password length of 72 bytes.
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 3) Attempt to store the new user
    if err := createUser(r.Context(), username, string(hashedPassword)); err != nil {
        if errors.Is(err, ErrUserExists) {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        log.Printf("createUser error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 4) Success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprint(w, "user registered")
}

// validateCredentials applies basic sanity checks on username & password.
func validateCredentials(username, password string) error {
    if len(username) == 0 || len(password) == 0 {
        return errors.New("username and password required")
    }
    if len(username) > maxUsernameLen {
        return fmt.Errorf("username too long (max %d characters)", maxUsernameLen)
    }
    if !validUsername.MatchString(username) {
        return errors.New("username may only contain letters, digits, and underscore")
    }
    if len(password) < minPasswordLen {
        return fmt.Errorf("password must be at least %d characters", minPasswordLen)
    }
    if len(password) > maxPasswordLen {
        return fmt.Errorf("password cannot exceed %d characters", maxPasswordLen)
    }
    return nil
}

// createUser tries to INSERT a new user. It returns ErrUserExists if the
// username is already in use.
func createUser(ctx context.Context, username, passwordHash string) error {
    // We wrap in a transaction to keep it atomic (optional here,
    // but useful if you have other related tables/constraints).
    tx, err := db.BeginTx(ctx, nil)
    if err != nil {
        return err
    }
    defer tx.Rollback() // safe to call even if we call tx.Commit()

    // Check for pre-existing user
    var exists bool
    row := tx.QueryRowContext(ctx, `
        SELECT EXISTS(
            SELECT 1 FROM users WHERE username = ?
        )`, username)
    if err := row.Scan(&exists); err != nil {
        return err
    }
    if exists {
        return ErrUserExists
    }

    // Insert the new user
    // We store only the bcrypt hash -- never the plaintext password.
    _, err = tx.ExecContext(ctx, `
        INSERT INTO users (username, password_hash, created_at)
        VALUES (?, ?, NOW())
    `, username, passwordHash)
    if err != nil {
        return err
    }

    return tx.Commit()
}