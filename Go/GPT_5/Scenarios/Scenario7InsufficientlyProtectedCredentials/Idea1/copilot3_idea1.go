package main

import (
    "context"
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // DB_DSN should be provided via environment variable, e.g.:
    // export DB_DSN="user:password@tcp(localhost:3306)/testdb?parseTime=true"
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        // fail fast in this example
        panic("DB_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        panic(err)
    }
    // optional: tune connection pool as needed
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(time.Minute * 5)

    // verify connection
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        panic(err)
    }

    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    // basic validation
    if len(password) < 8 {
        http.Error(w, "password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // check if username already exists
    var exists int
    err := db.QueryRowContext(ctx, "SELECT 1 FROM users WHERE username = ? LIMIT 1", username).Scan(&exists)
    if err != nil && err != sql.ErrNoRows {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if err == nil {
        http.Error(w, "username already taken", http.StatusConflict)
        return
    }

    // hash the password with bcrypt
    const cost = 12
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), cost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // insert the user using a parameterized statement
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashed))
    if err != nil {
        http.Error(w, "error registering user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}
