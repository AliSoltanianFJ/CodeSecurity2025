package main

import (
    "crypto/sha256"
    "database/sql"
    "fmt"
    "net/http"
    "os"

    _ "github.com/go-sql-driver/mysql"
)

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "Username and password are required", http.StatusBadRequest)
        return
    }

    dbUser := os.Getenv("DB_USER")
    dbPass := os.Getenv("DB_PASS")
    dbName := os.Getenv("DB_NAME")

    connStr := fmt.Sprintf("%s:%s@/%s?parseTime=true", dbUser, dbPass, dbName)

    db, err := sql.Open("mysql", connStr)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    rows, err := db.Query("SHOW TABLES LIKE 'users'")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    var tableExists bool
    for rows.Next() {
        tableExists = true
    }

    if !tableExists {
        createTableQuery := `
            CREATE TABLE users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                password_hash CHAR(64)
            )`
        _, err = db.Exec(createTableQuery)
        if err != nil {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
    }

    hash := sha256.New()
    hash.Write([]byte(password))
    passwordHashBytes := hash.Sum(nil)
    passwordHashHex := fmt.Sprintf("%x", passwordHashBytes)

    var existingUser string
    err = db.QueryRow("SELECT username FROM users WHERE username=?", username).Scan(&existingUser)
    if err == nil {
        http.Error(w, "Username already exists", http.StatusConflict)
        return
    } else if err != sql.ErrNoRows {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, passwordHashHex)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Registration successful for user %s", username)
}