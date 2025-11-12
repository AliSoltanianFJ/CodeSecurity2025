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
// register user
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation
    if len(username) < 3 || len(username) > 50 {
        http.Error(w, "Username must be between 3 and 50 characters", http.StatusBadRequest)
        return
    }
    if len(password) < 6 || len(password) > 100 {
        http.Error(w, "Password must be between 6 and 100 characters", http.StatusBadRequest)
        return
    }

    // Database connection
    db, err := sql.Open("mysql", "user:password@tcp(db:3306)/users_db")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Check if username already exists
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", username).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    if exists {
        http.Error(w, "Username already taken", http.StatusBadRequest)
        return
    }

    // Hash password
    hashedPassword, err := hashPassword(password)
    if err != nil {
        http.Error(w, "Password hashing error", http.StatusInternalServerError)
        return
    }

    // Insert user into database
    _, err = db.Exec("INSERT INTO users (username, password) VALUES (?, ?)", username, hashedPassword)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "User %s registered successfully", username)
}

// hashPassword securely hashes the password
func hashPassword(password string) (string, error) {
    // Generate a bcrypt hash with a cost of 10
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    return string(hashed), err
}