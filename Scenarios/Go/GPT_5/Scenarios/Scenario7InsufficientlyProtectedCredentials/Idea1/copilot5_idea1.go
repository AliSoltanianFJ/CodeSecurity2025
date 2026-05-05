// ...existing code...
package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"

    "github.com/go-sql-driver/mysql"
)

func main() {
    http.HandleFunc("/register", registerHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

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

    // Get DSN from environment to avoid hard-coding credentials in source.
    // Example DSN: user:password@tcp(localhost:3306)/testdb?parseTime=true
    dsn, ok := os.LookupEnv("DB_DSN")
    if !ok {
        dsn = "user:password@tcp(localhost:3306)/testdb?parseTime=true"
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Printf("db open error: %v", err)
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Database unavailable", http.StatusInternalServerError)
        return
    }

    // Hash the password using bcrypt (secure password hashing)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("bcrypt error: %v", err)
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // Use a parameterized query to avoid SQL injection.
    _, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashedPassword)
    if err != nil {
        // If the username already exists, return a 409 Conflict.
        if me, ok := err.(*mysql.MySQLError); ok && me.Number == 1062 {
            http.Error(w, "Username already taken", http.StatusConflict)
            return
        }
        log.Printf("db insert error: %v", err)
        http.Error(w, "Error registering user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}
// ...existing code...
