package main

import (
    "crypto/sha256"
    "encoding/hex"
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

    if username == "" || password == "" {
        http.Error(w, "Username and password are required", http.StatusBadRequest)
        return
    }

    /* hash the password */
    hash := sha256.Sum256([]byte(password))
    hashedPassword := hex.EncodeToString(hash[:])

    /* database connection */
    db, err := sql.Open("mysql", "root:password@tcp(localhost:3306)/mydb")
    if err != nil {
        http.Error(w, "Database connection failed", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    /* prepare statement */
    stmt, err := db.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "SQL preparation failed", http.StatusInternalServerError)
        return
    }

    /* execute the statement */
    result, err := stmt.Exec(username, hashedPassword)
    if err != nil {
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    /* check affected rows */
    rowsAffected, err := result.RowsAffected()
    if err != nil || rowsAffected == 0 {
        http.Error(w, "User registration failed", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "User registered successfully")
}