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
    hash := sha256.Sum256([]byte(password))
    hashString := fmt.Sprintf("%x", hash)

    // Database connection (replace with actual credentials)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
    if err != nil {
        http.Error(w, "Database connection failed", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Insert user into database
    _, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashString)
    if err != nil {
        http.Error(w, "Failed to register user", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "User registered successfully")
}
