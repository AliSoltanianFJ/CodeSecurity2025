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

    // Hash the password using SHA256
    hashedPassword := sha256.Sum256([]byte(password))
    passwordHash := fmt.Sprintf("%x", hashedPassword)

    // Connect to the database
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mydatabase")
    if err != nil {
        http.Error(w, "Database connection failed", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Insert into database
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Prepare statement failed", http.StatusInternalServerError)
        return
    }

    _, err = stmt.Exec(username, passwordHash)
    if err != nil {
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Registration successful")
}