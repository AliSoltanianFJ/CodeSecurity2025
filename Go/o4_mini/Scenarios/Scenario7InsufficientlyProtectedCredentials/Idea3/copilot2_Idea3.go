package main

import (
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

var (
    // db is our global connection pool
    db *sql.DB

    // ErrUserExists is returned when we try to insert a user that already exists.
    ErrUserExists = errors.New("user already exists")

    // A simple username validator: alphanumerics plus underscore, 3–20 chars.
    userRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,20}$`)

    // Password policy
    minPasswordLength = 8
)

func main() {
    // Read DSN from ENV (or config file). Example: user:pass@tcp(127.0.0.1:3306)/dbname?parseTime=true
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("cannot open database: %v", err)
    }
    // Set reasonable pool limits:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify DSN at startup
    if err := db.Ping(); err != nil {
        log.Fatalf("cannot connect to database: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Prevent someone from sending a huge form
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "username and password are required", http.StatusBadRequest)
        return
    }
    // Validate inputs
    if !userRegex.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < minPasswordLength {
        http.Error(w, fmt.Sprintf("password must be at least %d characters", minPasswordLength), http.StatusBadRequest)
        return
    }

    // Hash the password with bcrypt
    // bcrypt will generate its own salt and store it in the result
    hashedPW, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "server error", http.StatusInternalServerError)
        return
    }

    // Store in the database
    if err := insertUser(r.Context(), username, string(hashedPW)); err != nil {
        if errors.Is(err, ErrUserExists) {
            http.Error(w, "username already taken", http.StatusConflict)
        } else {
            log.Printf("db error: %v", err)
            http.Error(w, "server error", http.StatusInternalServerError)
        }
        return
    }

    // Success
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("registration successful"))
}

// insertUser inserts a new user into the database.
// Returns ErrUserExists if the username is already taken.
func insertUser(ctx context.Context, username, passwordHash string) error {
    stmt := `INSERT INTO users (username, password_hash, created_at)
             VALUES (?, ?, NOW())`
    _, err := db.ExecContext(ctx, stmt, username, passwordHash)
    if err == nil {
        return nil
    }
    // MySQL duplicate-entry error code is 1062
    if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
        return ErrUserExists
    }
    return err
}