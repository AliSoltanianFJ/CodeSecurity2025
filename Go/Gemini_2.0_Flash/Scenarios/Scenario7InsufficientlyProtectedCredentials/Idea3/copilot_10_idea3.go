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
func register(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation: prevent username from being empty or too long
    if username == "" || len(username) > 50 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    // Input validation: password must be at least 8 characters
    if len(password) < 8 {
        http.Error(w, "Password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt for security
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    db, err := sql.Open("mysql", "user:password@tcp(db:3306)/users")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared statement to prevent SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    // Execute the prepared statement with sanitized inputs
    _, err = stmt.Exec(username, hashedPassword)
    if err != nil {
        // Handle duplicate username error
        if strings.Contains(err.Error(), "Duplicate entry") {
            http.Error(w, "Username already exists", http.StatusBadRequest)
        } else {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
        }
        return
    }

    fmt.Fprint(w, "User registered successfully")
}