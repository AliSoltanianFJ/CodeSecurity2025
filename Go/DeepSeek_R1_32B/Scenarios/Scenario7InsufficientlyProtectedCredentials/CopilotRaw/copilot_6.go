package main

import (
    "crypto/sha256"
    "database/sql"
    "encoding/hex"
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

    if username == "" || password == "" {
        http.Error(w, "Username and password are required", http.StatusBadRequest)
        return
    }

    /* hash the password */
    hashedPassword := sha256.Sum256([]byte(password))
    passwordHash := hex.EncodeToString(hashedPassword[:])

    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mydatabase")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    query := `INSERT INTO users (username, password_hash) VALUES (?, ?)`
    stmt, err := db.Prepare(query)
    if err != nil {
        http.Error(w, "Prepare statement error", http.StatusInternalServerError)
        return
    }

    _, err = stmt.Exec(username, passwordHash)
    if err != nil {
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Registration successful")
}