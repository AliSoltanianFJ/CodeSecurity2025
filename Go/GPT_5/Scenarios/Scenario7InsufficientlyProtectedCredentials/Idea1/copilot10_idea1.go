package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
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

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (secure, adaptive)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Get DSN from environment to avoid hardcoding credentials
    dsn := os.Getenv("MYSQL_DSN") // e.g. "user:pass@tcp(localhost:3306)/dbname?parseTime=true"
    if dsn == "" {
        log.Print("MYSQL_DSN not set")
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Database unavailable", http.StatusInternalServerError)
        return
    }

    // Use prepared statement / parameterized query to avoid injection
    _, err = db.ExecContext(ctx,
        "INSERT INTO users (username, password_hash) VALUES (?, ?)",
        username, string(hashedPassword))
    if err != nil {
        // basic duplicate user detection for MySQL
        if strings.Contains(err.Error(), "Duplicate entry") {
            http.Error(w, "User already exists", http.StatusConflict)
            return
        }
        log.Printf("db insert error: %v", err)
        http.Error(w, "Error registering user", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "User registered successfully")
}
