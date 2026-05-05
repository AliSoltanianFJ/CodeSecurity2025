// ...existing code...
package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "strings"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)
// ...existing code...
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form values (defensive)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    if len(username) > 255 || len(password) > 1024 {
        http.Error(w, "input too long", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (secure, salted, adaptive)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "error processing password", http.StatusInternalServerError)
        return
    }

    // Get DSN from environment to avoid hardcoding credentials in source.
    // Example DSN: "user:pass@tcp(127.0.0.1:3306)/testdb"
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        dsn = "root:password@tcp(127.0.0.1:3306)/testdb"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        http.Error(w, "database unreachable", http.StatusInternalServerError)
        return
    }

    // Ensure users table exists. Store bcrypt hash in a binary/char column.
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password_hash VARBINARY(255) NOT NULL
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

    _, err = stmt.Exec(username, hashedPassword)
    if err != nil {
        // best-effort duplicate username handling
        if strings.Contains(err.Error(), "Duplicate") || strings.Contains(err.Error(), "1062") {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "could not create user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user created")
}
// ...existing code...
