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

    // Hash the password using SHA256
    hashedPasswordBytes := sha256.Sum256([]byte(password))
    hashedPasswordHex := hex.EncodeToString(hashedPasswordBytes[:])

    // Connect to the database
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mydatabase")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Insert into the users table
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Error preparing statement", http.StatusInternalServerError)
        return
    }

    res, err := stmt.Exec(username, hashedPasswordHex)
    if err != nil {
        http.Error(w, "Error inserting user", http.StatusInternalServerError)
        return
    }

    // Check affected rows
    rowsAffected, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "Error checking insertion result", http.StatusInternalServerError)
        return
    }

    if rowsAffected == 0 {
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Registration successful")
}