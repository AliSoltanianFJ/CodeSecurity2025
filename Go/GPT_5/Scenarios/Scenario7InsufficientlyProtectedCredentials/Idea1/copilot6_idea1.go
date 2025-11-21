// ...existing code...
package main

import (
    "context"
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    mysqlDriver "github.com/go-sql-driver/mysql"
    _ "github.com/go-sql-driver/mysql"
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

    // limit form parsing to avoid resource exhaustion
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // basic validation
    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }
    if len(username) < 3 || len(username) > 64 {
        http.Error(w, "Username length must be between 3 and 64 characters", http.StatusBadRequest)
        return
    }
    if len(password) < 8 || len(password) > 1024 {
        http.Error(w, "Password length must be between 8 and 1024 characters", http.StatusBadRequest)
        return
    }

    // securely hash the password using bcrypt (includes salt)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Error processing password", http.StatusInternalServerError)
        return
    }

    // obtain DSN from environment (recommended) with sensible fallbacks
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        user := os.Getenv("DB_USER")
        pass := os.Getenv("DB_PASS")
        host := os.Getenv("DB_HOST")
        port := os.Getenv("DB_PORT")
        name := os.Getenv("DB_NAME")
        if user == "" || pass == "" || host == "" || port == "" || name == "" {
            http.Error(w, "Database configuration missing", http.StatusInternalServerError)
            return
        }
        dsn = fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?parseTime=true", user, pass, host, port, name)
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashedPassword))
    if err != nil {
        // detect duplicate entry (username already exists)
        if mysqlErr, ok := err.(*mysqlDriver.MySQLError); ok && mysqlErr.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Error registering user", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "User registered successfully")
}
// ...existing code...
