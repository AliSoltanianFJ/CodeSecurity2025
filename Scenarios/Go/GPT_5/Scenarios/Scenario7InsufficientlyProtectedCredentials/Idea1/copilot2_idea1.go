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

    // Ensure form values are parsed
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }
    // Basic length checks
    if len(username) > 150 || len(password) > 1024 {
        http.Error(w, "Input too long", http.StatusBadRequest)
        return
    }

    // Hash the password with bcrypt (secure)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Get DSN from environment to avoid hardcoding credentials
    dsn := os.Getenv("DATABASE_DSN") // e.g. "user:pass@tcp(localhost:3306)/dbname?parseTime=true"
    if dsn == "" {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Optional: verify connection
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Database unavailable", http.StatusInternalServerError)
        return
    }

    // Insert the new user using a prepared statement to avoid injection
    ctx, cancel = context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    _, err = db.ExecContext(ctx, "INSERT INTO users (username, password_hash) VALUES (?, ?)", username, string(hashedPassword))
    if err != nil {
        // Handle duplicate username (MySQL returns "Error 1062: Duplicate entry ..." for unique key)
        if strings.Contains(err.Error(), "Duplicate entry") {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Error registering user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}
