package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
    "github.com/go-sql-driver/mysql"
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

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (secure, salted hashing)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "error processing password", http.StatusInternalServerError)
        return
    }

    // Build DSN from environment (preferred) or specific variables.
    // Set MYSQL_DSN or set MYSQL_USER, MYSQL_PASS, MYSQL_HOST, MYSQL_DB.
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        user := os.Getenv("MYSQL_USER")
        pass := os.Getenv("MYSQL_PASS")
        host := os.Getenv("MYSQL_HOST")
        dbname := os.Getenv("MYSQL_DB")
        if user == "" || host == "" || dbname == "" {
            http.Error(w, "database not configured", http.StatusInternalServerError)
            return
        }
        dsn = fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true", user, pass, host, dbname)
    }

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        http.Error(w, "database unreachable", http.StatusInternalServerError)
        return
    }

    // Ensure users table exists. Store bcrypt hash as VARBINARY.
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password_hash VARBINARY(255) NOT NULL
    )`)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashed)
    if err != nil {
        // detect duplicate username (MySQL error 1062)
        if mysqlErr, ok := err.(*mysql.MySQLError); ok && mysqlErr.Number == 1062 {
            http.Error(w, "username already exists", http.StatusConflict)
            return
        }
        http.Error(w, "could not create user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user created")
}
// ...existing code...
