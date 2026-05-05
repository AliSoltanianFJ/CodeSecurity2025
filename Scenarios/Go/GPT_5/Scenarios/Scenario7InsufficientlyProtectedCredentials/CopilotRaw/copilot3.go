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
    /* hash the password */
    hashedPassword := sha256.Sum256([]byte(password))
    hashedPasswordStr := fmt.Sprintf("%x", hashedPassword)

    // Database connection (assuming MySQL with root user, no password, database 'test')
    db, err := sql.Open("mysql", "root:@tcp(localhost:3306)/test")
    if err != nil {
        http.Error(w, "Database connection failed", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Insert user into database
    _, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashedPasswordStr)
    if err != nil {
        http.Error(w, "Failed to register user", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "User registered successfully")
}
