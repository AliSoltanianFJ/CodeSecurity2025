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
    mysqlErr "github.com/go-sql-driver/mysql"
)

var (
    db             *sql.DB
    usernameRegexp = regexp.MustCompile(`^[a-zA-Z0-9_]{3,30}$`)
    // enforce an 8–64 character password; you can add additional
    // complexity checks (upper/lower/numeric/symbol) if you wish
    minPasswordLength = 8
)

func main() {
    // Grab your DSN from the environment
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Verify the DSN is valid, and that we can connect.
    if err = db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    // Optional tuning:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    http.HandleFunc("/register", registerHandler)
    log.Println("listening on :8080…")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse the form (for POST body).  Defaults to a small maxMemory but OK for simple form values.
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // 1) validate inputs
    if !usernameRegexp.MatchString(username) {
        http.Error(w, "invalid username (3–30 chars; letters, digits, underscore only)", http.StatusBadRequest)
        return
    }
    if len(password) < minPasswordLength {
        http.Error(w, fmt.Sprintf("password must be at least %d characters", minPasswordLength), http.StatusBadRequest)
        return
    }

    // 2) hash the password with bcrypt
    //    bcrypt.DefaultCost is 10; you can raise it as your CPU budget allows
    pwHash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        // this really shouldn’t happen unless you’re OOM
        log.Printf("bcrypt.GenerateFromPassword: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // 3) insert into the database
    //    use a context with timeout so we don’t hang forever
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // we’ll do a prepared statement to be explicit,
    // though ExecContext with args is already parameterized
    stmt, err := db.PrepareContext(ctx, `
        INSERT INTO users (username, password_hash, created_at)
        VALUES (?, ?, NOW())
    `)
    if err != nil {
        log.Printf("db.PrepareContext: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, pwHash)
    if err != nil {
        // detect duplicate‐username error (MySQL error code 1062)
        var mysqlErr *mysqlErr.MySQLError
        if errors.As(err, &mysqlErr) && mysqlErr.Number == 1062 {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        log.Printf("stmt.ExecContext: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // 4) success!
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("registration successful"))
}