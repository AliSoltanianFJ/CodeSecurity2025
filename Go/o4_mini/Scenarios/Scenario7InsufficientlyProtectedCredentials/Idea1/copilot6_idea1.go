package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "os"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // basic validation
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // hash the password with bcrypt (secure adaptive hashing)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // get DSN from environment to avoid hardcoding credentials
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        http.Error(w, "database not configured", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // ensure a simple users table exists (id, username unique, password_hash)
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

    _, err = stmt.Exec(username, hashed)
    if err != nil {
        // detect duplicate username (MySQL error 1062)
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
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
