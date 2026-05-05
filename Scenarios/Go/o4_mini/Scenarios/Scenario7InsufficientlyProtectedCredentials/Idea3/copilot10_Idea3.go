package main

import (
    "context"
    "database/sql"
    "errors"
    "fmt"
    "log"
    "net/http"
    "strings"
    "time"

    mysqlDriver "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func main() {
    var err error
    // Adjust the DSN to your credentials/hosts/etc.
    dsn := "user:password@tcp(db-host:3306)/yourdbname?parseTime=true&loc=UTC"
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open database: %v", err)
    }
    // Set reasonable connection‐pool limits:
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    // Verify DSN at startup.
    if err = db.Ping(); err != nil {
        log.Fatalf("unable to reach database: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Println("listening on :8080 …")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 1) Parse & sanitize
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // 2) Validate your inputs
    if len(username) < 3 || len(username) > 50 {
        http.Error(w, "username must be between 3 and 50 characters", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // 3) Hash the password with bcrypt (it generates its own per-user salt)
    //    bcrypt.DefaultCost is 10; you can raise it to 12–14 as hardware improves.
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("bcrypt failure: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 4) Insert into the database using a context with timeout and a parameterized query.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    query := `INSERT INTO users (username, password_hash) VALUES (?, ?)`
    _, err = db.ExecContext(ctx, query, username, hashedPassword)
    if err != nil {
        // 5) If it’s a duplicate‐key error, mysql returns error number 1062
        var mErr *mysqlDriver.MySQLError
        if errors.As(err, &mErr) && mErr.Number == 1062 {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        log.Printf("db insert error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 6) Success!
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user registered")
}