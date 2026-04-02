package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // Replace with your own connection string or better yet,
    // pull from environment variables / a secrets manager.
    // Ensure your DSN enables the proper charset / collation for your app.
    dsn := "appuser:apppassword@tcp(127.0.0.1:3306)/appdb?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci"

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    // It's a good idea to tune these in a production setting:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    if err = db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    log.Println("Listening on :8080…")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

var (
    // Only allow usernames made of 3–30 alphanumeric chars + _ or -
    usernameRegexp = regexp.MustCompile(`^[a-zA-Z0-9_-]{3,30}$`)
    // Bcrypt strength.  DefaultCost is 10; you can raise it, at the cost of CPU/time.
    bcryptCost = bcrypt.DefaultCost
)

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // ParseForm populates r.Form and r.PostForm
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation
    if username == "" || password == "" {
        http.Error(w, "username and password are required", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password must be at least 8 characters", http.StatusBadRequest)
        return
    }
    if !usernameRegexp.MatchString(username) {
        http.Error(w, "username must be 3–30 chars, alphanumeric or _ -", http.StatusBadRequest)
        return
    }

    // Hash the password with bcrypt
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcryptCost)
    if err != nil {
        // Never reveal internal errors to the client
        log.Printf("bcrypt.GenerateFromPassword: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Insert the new user.  We assume you have:
    //
    //   CREATE TABLE users (
    //     id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    //     username        VARCHAR(30) NOT NULL UNIQUE,
    //     password_hash   VARBINARY(60) NOT NULL,
    //     created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    //   );
    //
    stmt := `INSERT INTO users (username, password_hash) VALUES (?, ?)`
    _, err = db.Exec(stmt, username, hashedPassword)
    if err != nil {
        // Check for duplicate‐key error (MySQL error 1062)
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "username already taken", http.StatusConflict)
            return
        }
        log.Printf("db.Exec: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user registered successfully")
}