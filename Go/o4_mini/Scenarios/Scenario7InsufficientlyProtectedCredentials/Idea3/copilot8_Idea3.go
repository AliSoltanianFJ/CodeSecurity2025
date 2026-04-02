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
    // db is our application-wide connection pool.
    db *sql.DB

    // ErrUserExists is returned when a user already exists in the DB.
    ErrUserExists = errors.New("user already exists")

    // a simple username validation: letters, numbers and underscore, 3–30 chars
    usernameRegexp = regexp.MustCompile(`^[a-zA-Z0-9_]{3,30}$`)
)

func main() {
    // Initialize our database.
    if err := initDB(); err != nil {
        log.Fatalf("could not initialize database: %v", err)
    }
    defer db.Close()

    http.HandleFunc("/register", registerHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// initDB reads the DSN from MYSQL_DSN (e.g. "user:pass@tcp(127.0.0.1:3306)/mydb?parseTime=true")
// and configures a connection pool.
func initDB() error {
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        return fmt.Errorf("MYSQL_DSN environment variable is not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        return fmt.Errorf("sql.Open: %w", err)
    }

    // Configure your pool as needed:
    db.SetConnMaxLifetime(3 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify that the database is reachable.
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        return fmt.Errorf("db.Ping: %w", err)
    }

    return nil
}

// registerHandler handles POST /register
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit how large a request body we'll read (e.g. 10KB)
    r.Body = http.MaxBytesReader(w, r.Body, 10_000)

    // ParseForm populates r.Form and r.PostForm
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostForm.Get("username")
    password := r.PostForm.Get("password")

    // Validate username and password
    if !usernameRegexp.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // Hash the password with bcrypt
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("bcrypt.GenerateFromPassword: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Insert the new user into the database
    err = insertUser(r.Context(), username, string(hashedPassword))
    if err != nil {
        if errors.Is(err, ErrUserExists) {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        log.Printf("insertUser error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("user registered"))
}

// insertUser attempts to insert a new user. Returns ErrUserExists if the username is already taken.
func insertUser(ctx context.Context, username, passwordHash string) error {
    // Give this DB call a timeout.
    ctx, cancel := context.WithTimeout(ctx, 3*time.Second)
    defer cancel()

    // We assume you have a table like:
    //
    // CREATE TABLE users (
    //     id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    //     username      VARCHAR(30) NOT NULL UNIQUE,
    //     password_hash CHAR(60)    NOT NULL,
    //     created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
    // );
    //
    query := `INSERT INTO users (username, password_hash) VALUES (?, ?)`

    _, err := db.ExecContext(ctx, query, username, passwordHash)
    if err == nil {
        return nil
    }

    // If it's a MySQL "duplicate entry" error, wrap it:
    //
    //   Error 1062: Duplicate entry 'foobar' for key 'users.username'
    //
    // you can also do more sophisticated inspection by casting to
    // *mysql.MySQLError, but string‐matching on "1062" is sufficient here.
    if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
        return ErrUserExists
    }

    return err
}