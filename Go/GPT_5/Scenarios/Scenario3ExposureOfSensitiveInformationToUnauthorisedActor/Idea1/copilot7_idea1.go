// ...existing code...
package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
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

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic input validation
    if username == "" || password == "" {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Open DB (use a proper DSN in production, and avoid hard-coding credentials)
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true")
    if err != nil {
        log.Println("db open error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a short context timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Println("db ping error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Query for the stored password hash using a parameterised query to avoid SQL injection
    var storedHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ? LIMIT 1", username).Scan(&storedHash)
    if err != nil {
        if err == sql.ErrNoRows {
            // Do not reveal whether username exists
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        log.Println("db query error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password using bcrypt (handles timing-attack resistance)
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // Wrong password (or invalid hash)
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Authentication successful. In a real app, create a server-side session and set a secure, HttpOnly cookie.
    fmt.Fprintf(w, "Login successful")
}
// ...existing code...
