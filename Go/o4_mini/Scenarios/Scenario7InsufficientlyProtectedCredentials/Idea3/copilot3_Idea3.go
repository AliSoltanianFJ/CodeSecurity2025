package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "errors"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
    mysqlErr "github.com/go-sql-driver/mysql"
)

var (
    db              *sql.DB
    // simple username regexp: alphanumeric + underscore, 3–30 chars
    usernameRegexp  = regexp.MustCompile(`^[a-zA-Z0-9_]{3,30}$`)
    minPasswordLen  = 8
)

func main() {
    var err error
    // it's best to pull your DSN from an env var or config
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        fmt.Println("Please set MYSQL_DSN")
       	os.Exit(1)
    }

    // db is a *sql.DB, which is a pool of connections
    db, err = sql.Open("mysql", dsn+"?parseTime=true&charset=utf8mb4")
    if err != nil {
        fmt.Printf("sql.Open: %v\n", err)
        os.Exit(1)
    }
    // Set sensible limits for the pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // verify connectivity
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        fmt.Printf("db.Ping: %v\n", err)
        os.Exit(1)
    }

    http.HandleFunc("/register", registerHandler)
    fmt.Println("Listening on :8080")
    http.ListenAndServe(":8080", nil)
}

type errorResponse struct {
    Error string `json:"error"`
}

type successResponse struct {
    Message string `json:"message"`
}

func writeJSON(w http.ResponseWriter, code int, payload interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(code)
    json.NewEncoder(w).Encode(payload)
}

// registerHandler handles POST /register
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        writeJSON(w, http.StatusMethodNotAllowed, errorResponse{"method not allowed"})
        return
    }

    // limit the size of the request body to avoid DOS
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if err := r.ParseForm(); err != nil {
        writeJSON(w, http.StatusBadRequest, errorResponse{"invalid form data"})
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // 1. Basic validation
    if username == "" || password == "" {
        writeJSON(w, http.StatusBadRequest, errorResponse{"username and password required"})
        return
    }
    if !usernameRegexp.MatchString(username) {
        writeJSON(w, http.StatusBadRequest, errorResponse{"invalid username"})
        return
    }
    if len(password) < minPasswordLen {
        writeJSON(w, http.StatusBadRequest, errorResponse{fmt.Sprintf("password must be at least %d characters", minPasswordLen)})
        return
    }

    // 2. Hash the password with bcrypt (it generates its own salt)
    //    Bcrypt cost 12 is a reasonable default; adjust for your hardware
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), 12)
    if err != nil {
        // unexpected error
        writeJSON(w, http.StatusInternalServerError, errorResponse{"failed to hash password"})
        return
    }

    // 3. Insert into the database
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // We'll rely on a UNIQUE constraint on the username column.
    query := `INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, UTC_TIMESTAMP())`
    _, err = db.ExecContext(ctx, query, username, string(hashedPassword))
    if err != nil {
        // check for MySQL duplicate‐key error
        var mysqlErr *mysqlErr.MySQLError
        if errors.As(err, &mysqlErr) && mysqlErr.Number == 1062 {
            writeJSON(w, http.StatusConflict, errorResponse{"username already exists"})
            return
        }
        // Some other DB error
        fmt.Printf("db insert: %v\n", err)
        writeJSON(w, http.StatusInternalServerError, errorResponse{"unable to create user"})
        return
    }

    // 4. Everything went fine
    writeJSON(w, http.StatusCreated, successResponse{"user registered successfully"})
}