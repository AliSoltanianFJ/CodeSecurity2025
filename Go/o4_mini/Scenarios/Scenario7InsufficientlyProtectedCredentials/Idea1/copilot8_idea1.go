package main

import (
    "context"
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"
    mysql "github.com/go-sql-driver/mysql"
)

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

    // parse form values
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // basic validation
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    if len(username) > 255 {
        http.Error(w, "username too long", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // bcrypt the password (secure hashing with salt)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // DSN should be provided via environment variable MYSQL_DSN (fallback for local dev)
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

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    // ensure users table exists
    _, err = db.ExecContext(ctx, `CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password_hash VARBINARY(255) NOT NULL
    )`)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, hashed)
    if err != nil {
        // detect duplicate username for MySQL
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user created")
}
