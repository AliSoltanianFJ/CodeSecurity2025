package main

import (
    "context"
    "database/sql"
    "fmt"
    "net/http"
    "os"
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

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }

    if len(password) < 8 {
        http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // Hash the password securely using bcrypt
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Build DSN from environment or use DB_DSN
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        user := os.Getenv("DB_USER")
        pass := os.Getenv("DB_PASS")
        host := os.Getenv("DB_HOST")
        name := os.Getenv("DB_NAME")
        if user == "" || pass == "" || host == "" || name == "" {
            http.Error(w, "Server configuration error", http.StatusInternalServerError)
            return
        }
        dsn = fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true", user, pass, host, name)
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Insert user (using placeholder parameters to avoid injection)
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashedPassword))
    if err != nil {
        http.Error(w, "Error registering user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}
// ...existing code...
