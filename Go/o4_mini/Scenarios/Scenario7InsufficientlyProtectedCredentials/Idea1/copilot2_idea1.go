// ...existing code...
package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "strings"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)
// ...existing code...

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // basic validation
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    if len(username) > 255 || len(username) < 3 {
        http.Error(w, "username length invalid", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password too short (min 8 chars)", http.StatusBadRequest)
        return
    }

    // bcrypt the password (secure password hashing)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "could not hash password", http.StatusInternalServerError)
        return
    }

    // NOTE: update DSN to match your environment
    db, err := sql.Open("mysql", "root:password@tcp(127.0.0.1:3306)/testdb")
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        http.Error(w, "database unreachable", http.StatusInternalServerError)
        return
    }

    // ensure a simple users table exists (id, username unique, password_hash)
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL
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
        // simple duplicate check without importing driver-specific types
        if strings.Contains(err.Error(), "Duplicate entry") {
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
