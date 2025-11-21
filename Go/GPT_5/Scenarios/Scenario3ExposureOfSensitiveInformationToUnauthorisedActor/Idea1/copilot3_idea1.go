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
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // minimal input handling
    username := r.FormValue("username")
    password := r.FormValue("password")

    // Get DSN from environment (safer than hardcoding); fallback only for convenience
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    // short context to avoid long DB hangs
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared query and parameter binding to avoid injection
    const query = "SELECT password_hash FROM users WHERE username = ? LIMIT 1"

    var storedHash string
    err = db.QueryRowContext(ctx, query, username).Scan(&storedHash)
    if err != nil {
        // Do not reveal whether the user exists
        if err == sql.ErrNoRows {
            http.Error(w, "Invalid username or password", http.StatusUnauthorized)
            return
        }
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password using bcrypt (assumes storedHash is a bcrypt hash)
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Authentication successful — respond without leaking sensitive details
    fmt.Fprintf(w, "Welcome, %s! Login successful.\n", username)
}
