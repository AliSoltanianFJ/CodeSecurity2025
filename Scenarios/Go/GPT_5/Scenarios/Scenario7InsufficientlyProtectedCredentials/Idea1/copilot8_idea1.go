// ...existing code...
package main

import (
    "context"
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
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

    // Basic form parsing and validation
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
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

    // Build DSN from environment variables to avoid hard-coded credentials
    dbUser := os.Getenv("DB_USER")
    dbPass := os.Getenv("DB_PASS")
    dbHost := os.Getenv("DB_HOST") // e.g. "localhost:3306"
    dbName := os.Getenv("DB_NAME")
    if dbUser == "" || dbPass == "" || dbHost == "" || dbName == "" {
        http.Error(w, "Database configuration missing", http.StatusInternalServerError)
        return
    }
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&charset=utf8mb4,utf8", dbUser, dbPass, dbHost, dbName)

    // Open DB (in production prefer a shared *sql.DB reused across requests)
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Database unreachable", http.StatusInternalServerError)
        return
    }

    // Check for existing username
    var exists int
    err = db.QueryRowContext(ctx, "SELECT COUNT(1) FROM users WHERE username = ?", username).Scan(&exists)
    if err != nil {
        http.Error(w, "Error checking existing user", http.StatusInternalServerError)
        return
    }
    if exists > 0 {
        http.Error(w, "Username already taken", http.StatusConflict)
        return
    }

    // Hash the password securely using bcrypt
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Error processing password", http.StatusInternalServerError)
        return
    }

    // Insert the user using parameterized query
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashedPassword))
    if err != nil {
        http.Error(w, "Error registering user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}
// ...existing code...
