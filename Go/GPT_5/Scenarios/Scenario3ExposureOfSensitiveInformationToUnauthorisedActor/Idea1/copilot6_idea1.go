// ...existing code...
package main

import (
    "context"
    "database/sql"
    "net/http"
    "os"
    "strings"
    "time"
    "log"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
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

    // Parse form values
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Missing credentials", http.StatusBadRequest)
        return
    }

    // Get DSN from environment to avoid hardcoding credentials
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        http.Error(w, "Server configuration error", http.StatusInternalServerError)
        log.Println("missing DB_DSN environment variable")
        return
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db open error:", err)
        return
    }
    defer db.Close()

    // Use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Verify connection
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db ping error:", err)
        return
    }

    // Use a parameterised query to avoid SQL injection
    const query = "SELECT password_hash FROM users WHERE username = ?"
    var passwordHash string
    err = db.QueryRowContext(ctx, query, username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        // Do not reveal whether username or password was incorrect
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        log.Println("db query error:", err)
        return
    }

    // Compare stored bcrypt hash with the provided password
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Successful login
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
