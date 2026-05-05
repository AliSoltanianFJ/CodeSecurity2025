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
    mysqlErr "github.com/go-sql-driver/mysql"
)

var (
    // db is a shared connection pool
    db *sql.DB

    // simple username validator: alphanumeric + underscores, 3–30 chars
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,30}$`)
)

func main() {
    // Read DSN from environment
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("Please set MYSQL_DSN environment variable")
    }

    var err error
    // Open doesn't actually connect yet
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Configure the connection pool
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify connectivity
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/register", registerHandler)

    log.Printf("Listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Only POST allowed
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form data (also handles URL-encoded bodies)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form", http.StatusBadRequest)
        return
    }

    username := r.Form.Get("username")
    password := r.Form.Get("password")

    // Validate input
    if !usernameRegex.MatchString(username) {
        http.Error(w, "invalid username (use 3–30 alphanumeric characters or underscores)", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    //Register user
    if err := register(r.Context(), username, password); err != nil {
        switch {
        case errors.Is(err, ErrUserExists):
            http.Error(w, "username already taken", http.StatusConflict)
        default:
            log.Printf("register error: %v", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
        }
        return
    }

    // Success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user created")
}

var ErrUserExists = errors.New("user already exists")

// register hashes the password and inserts a new user into the database.
// Returns ErrUserExists if the username is already taken.
func register(parentCtx context.Context, username, password string) error {
    // Derive a new context with timeout for the database operation
    ctx, cancel := context.WithTimeout(parentCtx, 3*time.Second)
    defer cancel()

    // 1) Hash the password with bcrypt (automatically generates a salt).
    //    DefaultCost is currently 10; you can increase this over time.
    hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        return fmt.Errorf("bcrypt: %w", err)
    }

    // 2) Use a prepared statement for the INSERT
    stmt, err := db.PrepareContext(ctx,
        "INSERT INTO users (username, password_hash) VALUES (?, ?)",
    )
    if err != nil {
        return fmt.Errorf("prepare statement: %w", err)
    }
    defer stmt.Close()

    // 3) Execute
    _, err = stmt.ExecContext(ctx, username, hash)
    if err != nil {
        // MySQL duplicate-entry error code = 1062
        var me *mysqlErr.MySQLError
        if errors.As(err, &me) && me.Number == 1062 {
            return ErrUserExists
        }
        return fmt.Errorf("exec insert: %w", err)
    }

    return nil
}