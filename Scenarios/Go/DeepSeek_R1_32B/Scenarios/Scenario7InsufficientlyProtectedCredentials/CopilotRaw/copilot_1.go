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

    // Hash the password
    hasher := sha256.New()
    hasher.Write([]byte(password))
    hashedPasswordBytes := hasher.Sum(nil)
    hashedPassword := hex.EncodeToString(hashedPasswordBytes)

    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Check if username already exists
    query := `SELECT 1 FROM users WHERE username = ?`
    var exists int
    err = db.QueryRow(query, username).Scan(&exists)
    if err == nil {
        http.Error(w, "Username already exists", http.StatusConflict)
        return
    }
    if err != sql.ErrNoRows {
        http.Error(w, "Database query error", http.StatusInternalServerError)
        return
    }

    // Insert new user into database
    insertStmt := `INSERT INTO users (username, password) VALUES (?, ?)`
    res, err := db.Exec(insertStmt, username, hashedPassword)
    if err != nil {
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    id, err := res.LastInsertId()
    if err != nil {
        http.Error(w, "Error retrieving user ID", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "User registered successfully. User ID: %d\n", id)
}