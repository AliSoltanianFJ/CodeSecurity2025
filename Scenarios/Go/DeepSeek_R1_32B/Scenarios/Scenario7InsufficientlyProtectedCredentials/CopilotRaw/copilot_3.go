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

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Hash the password using SHA-256
    hasher := sha256.New()
    hasher.Write([]byte(password))
    hashedPasswordBytes := hasher.Sum(nil)
    hashedPassword, _ := hex.EncodeToString(hashedPasswordBytes)

    // Connect to the database
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Check if the username already exists
    var existingUsername string
    err = db.QueryRow("SELECT username FROM users WHERE username=?", username).Scan(&existingUsername)
    if err == sql.ErrNoRows {
        // Username does not exist, proceed to insert
        stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
        if err != nil {
            http.Error(w, "Database error", http.StatusInternalServerError)
            return
        }
        defer stmt.Close()

        res, err := stmt.Exec(username, hashedPassword)
        if err != nil {
            http.Error(w, "Registration failed", http.StatusInternalServerError)
            return
        }

        // Check if the insertion was successful
        rowsAffected, err := res.RowsAffected()
        if err != nil || rowsAffected != 1 {
            http.Error(w, "Registration failed", http.StatusInternalServerError)
            return
        }
    } else if err == nil {
        // Username already exists
        http.Error(w, "Username already taken", http.StatusConflict)
        return
    }

    // Send success response
    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Registration successful")
}