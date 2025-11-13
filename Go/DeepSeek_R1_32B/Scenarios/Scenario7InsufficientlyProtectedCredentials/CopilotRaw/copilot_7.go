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

    // Hash the password using SHA256
    hashedPassword := sha256.Sum256([]byte(password))
    hashedPasswordHex := hex.EncodeToString(hashedPassword[:])

    // Connect to the database
    db, err := sql.Open("mysql", "root:password@tcp(localhost:3306)/test")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Check if the connection is successful
    err = db.Ping()
    if err != nil {
        http.Error(w, "Database ping error", http.StatusInternalServerError)
        return
    }

    // Insert new user into database
    stmt, err := db.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Prepare statement failed", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    result, err := stmt.Exec(username, hashedPasswordHex)
    if err != nil {
        if e, ok := err.(*sql.NoRows); !ok && e != nil {
            http.Error(w, "Duplicate username or database error", http.StatusConflict)
        } else {
            http.Error(w, "Database insertion failed", http.StatusInternalServerError)
        }
        return
    }

    // Get the last inserted ID
    id, err := result.LastInsertId()
    if err != nil {
        http.Error(w, "Failed to retrieve user ID", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, `{"status": "success", "message": "User registered successfully", "user_id": %d}`, id)
}