// ...existing code...
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

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "Missing credentials", http.StatusBadRequest)
        return
    }

    // Use DSN from environment when possible to avoid hard-coded credentials.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        dsn = "root:root@tcp(localhost:3306)/db"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Small timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    if err := db.PingContext(ctx); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a parameterised query to avoid SQL injection.
    var storedHash string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        // Don't reveal whether username or password was incorrect.
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password with bcrypt hashed password stored in DB.
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    fmt.Fprintf(w, "Welcome, %s! Login successful.\n", username)
}
// ...existing code...
