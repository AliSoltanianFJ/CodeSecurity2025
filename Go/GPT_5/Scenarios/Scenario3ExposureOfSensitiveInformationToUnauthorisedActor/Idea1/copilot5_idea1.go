package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...

var db *sql.DB

func main() {
    // initialize DB (DSN from env if available, fallback to local)
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db?parseTime=true"
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }
    // set a reasonable connection lifetime / pool settings
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // verify connectivity
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("failed to ping db: %v", err)
    }
    defer db.Close()

    http.HandleFunc("/login", loginHandler)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

// ...existing code...

// loginHandler logs in the user using a parameterized query and bcrypt password verification.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form values
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Use a short context derived from the request to avoid long DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    hashed, err := fetchPasswordHash(ctx, username)
    if err != nil {
        // Do not reveal whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Compare hashed password securely
    if err := bcrypt.CompareHashAndPassword([]byte(hashed), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authentication successful - issue session/token here in real app.
    fmt.Fprintln(w, "Login successful")
}

// fetchPasswordHash retrieves the stored password hash for a username using a parameterized query.
func fetchPasswordHash(ctx context.Context, username string) (string, error) {
    const query = "SELECT password_hash FROM users WHERE username = ? LIMIT 1"
    var hash sql.NullString
    err := db.QueryRowContext(ctx, query, username).Scan(&hash)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            return "", err
        }
        return "", err
    }
    if !hash.Valid || hash.String == "" {
        return "", sql.ErrNoRows
    }
    return hash.String, nil
}
