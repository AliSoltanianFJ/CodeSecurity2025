package main

import (
    "crypto/sha256"
    "database/sql"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/database")
    if err != nil {
        panic(err)
    }
    defer db.Close()

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
    passwordHash := fmt.Sprintf("%x", hash)

    // Insert into database (assuming a table 'users' with columns 'username' and 'password_hash')
    _, err := db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, passwordHash)
    if err != nil {
        http.Error(w, "Failed to register", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "User registered successfully")
}
