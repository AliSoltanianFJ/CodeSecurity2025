package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
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

    /* validate input */
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    /* hash the password using bcrypt (slow, salted) */
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // DSN should be provided via environment in production.
    // Example DSN: user:pass@tcp(127.0.0.1:3306)/testdb
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "root:password@tcp(127.0.0.1:3306)/testdb" // update for your environment
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }

    // ensure a simple users table exists (id, username unique, password_hash)
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password_hash VARCHAR(60) NOT NULL
    )`)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, string(hashed))
    if err != nil {
        // avoid leaking details; detect common unique-constraint message to return 409
        if strings.Contains(err.Error(), "Duplicate entry") {
            http.Error(w, "could not create user", http.StatusConflict)
            return
        }
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user created")
}
// ...existing code...
