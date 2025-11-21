package main

import (
    "crypto/sha256"
    "database/sql"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
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

    // Hash the password using SHA-256
    hash := sha256.Sum256([]byte(password))
    passwordHash := fmt.Sprintf("%x", hash)

    // Connect to the database (assuming MySQL with root user, no password, database 'test')
    db, err := sql.Open("mysql", "root:@tcp(127.0.0.1:3306)/test")
    if err != nil {
        http.Error(w, "Database connection failed", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Insert the username and hashed password into the users table
    // Assuming the table is created as: CREATE TABLE users (username VARCHAR(255) PRIMARY KEY, password_hash VARCHAR(255));
    _, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, passwordHash)
    if err != nil {
        http.Error(w, "Failed to register user", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "User registered successfully")
}
