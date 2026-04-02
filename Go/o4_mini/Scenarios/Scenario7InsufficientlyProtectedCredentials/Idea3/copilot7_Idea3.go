package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var (
    db          *sql.DB
    userPattern = regexp.MustCompile(`^[a-zA-Z0-9_]{3,30}$`)
)

// initialize the database connection pool
func init() {
    // e.g. DSN:  user:password@tcp(host:port)/dbname?parseTime=true
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        panic("MYSQL_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        panic(fmt.Sprintf("failed to open database: %v", err))
    }

    // Set reasonable limits on the connection pool
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify connectivity
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        panic(fmt.Sprintf("failed to ping database: %v", err))
    }
}

func main() {
    http.HandleFunc("/register", registerHandler)
    fmt.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        panic(err)
    }
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse a maximum of, say, 1KB of form data
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Call our reusable register() function
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    if err := register(ctx, db, username, password); err != nil {
        switch {
        case errors.Is(err, ErrInvalidUsername), errors.Is(err, ErrInvalidPassword):
            http.Error(w, err.Error(), http.StatusBadRequest)
        case errors.Is(err, ErrUserExists):
            http.Error(w, err.Error(), http.StatusConflict)
        default:
            // for all other errors, return a generic 500
            fmt.Printf("internal error during register: %v\n", err)
            http.Error(w, "internal server error", http.StatusInternalServerError)
        }
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("user registered"))
}

// package-level errors for classification
var (
    ErrInvalidUsername = errors.New("username must be 3–30 chars, letters/numbers/underscore only")
    ErrInvalidPassword = errors.New("password must be at least 8 characters")
    ErrUserExists      = errors.New("user already exists")
)

// register performs all the DB work to create a new user record.
// It returns ErrInvalidUsername, ErrInvalidPassword, ErrUserExists, or a wrapped internal error.
func register(ctx context.Context, db *sql.DB, username, password string) error {
    // 1) Validate inputs
    if !userPattern.MatchString(username) {
        return ErrInvalidUsername
    }
    if len(password) < 8 {
        return ErrInvalidPassword
    }

    // 2) Check for existing user
    const checkUserSQL = `SELECT 1 FROM users WHERE username = ? LIMIT 1`
    var exists int
    err := db.QueryRowContext(ctx, checkUserSQL, username).Scan(&exists)
    if err == nil {
        // user already exists
        return ErrUserExists
    }
    if err != sql.ErrNoRows {
        // some other error occurred
        return fmt.Errorf("checking existing user: %w", err)
    }

    // 3) Hash the password using bcrypt
    //    bcrypt.DefaultCost is 10; you can bump it to 12 if your hardware permits.
    hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        return fmt.Errorf("password hashing failed: %w", err)
    }

    // 4) Insert the new user
    const insertUserSQL = `
        INSERT INTO users (username, password_hash, created_at)
        VALUES (?, ?, UTC_TIMESTAMP())
    `
    // Use ExecContext so we get both context cancellation and Exec’s error
    if _, err := db.ExecContext(ctx, insertUserSQL, username, hash); err != nil {
        // MySQL error code 1062 = duplicate key
        if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
            return ErrUserExists
        }
        return fmt.Errorf("inserting new user: %w", err)
    }

    return nil
}