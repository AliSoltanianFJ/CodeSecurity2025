package main

import (
    "context"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/register", registerHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // ensure form is parsed
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // basic validation
    if username == "" || password == "" {
        http.Error(w, "Username and password required", http.StatusBadRequest)
        return
    }
    if len(username) > 150 || len(password) > 1024 {
        http.Error(w, "Input too long", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (includes a salt)
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Error processing password", http.StatusInternalServerError)
        return
    }

    // Build DSN from environment variables to avoid hard-coding credentials.
    // Expected format: user:password@tcp(host:port)/dbname?parseTime=true
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        // fallback to composing from parts (optional)
        user := os.Getenv("MYSQL_USER")
        pass := os.Getenv("MYSQL_PASSWORD")
        host := os.Getenv("MYSQL_HOST")
        dbname := os.Getenv("MYSQL_DB")
        if user == "" || pass == "" || host == "" || dbname == "" {
            http.Error(w, "Server configuration error", http.StatusInternalServerError)
            return
        }
        dsn = fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true", user, pass, host, dbname)
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Set reasonable limits for the connection pool
    db.SetConnMaxIdleTime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    // use a context with timeout for DB operations
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // verify connectivity
    if err := db.PingContext(ctx); err != nil {
        http.Error(w, "Database unreachable", http.StatusInternalServerError)
        return
    }

    // Insert user using a parameterized query to avoid SQL injection.
    // Use a column name like password_hash to store the bcrypt output.
    // Adjust column name if your schema differs.
    query := "INSERT INTO users (username, password_hash) VALUES (?, ?)"
    _, err = db.ExecContext(ctx, query, username, string(hashedPassword))
    if err != nil {
        // handle duplicate username (MySQL error code 1062)
        if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
            http.Error(w, "Username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "Error registering user", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "User registered successfully")
}
