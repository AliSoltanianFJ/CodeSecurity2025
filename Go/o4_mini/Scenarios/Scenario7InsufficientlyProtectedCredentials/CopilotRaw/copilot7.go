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

// ...existing code...
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")
    /* hash the password */

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // Compute SHA-256 hash of the password
    sum := sha256.Sum256([]byte(password))
    passwordHash := fmt.Sprintf("%x", sum[:])

    // Open database connection (update DSN with real credentials/DB)
    db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/testdb")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a prepared statement to avoid injection
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, passwordHash)
    if err != nil {
        http.Error(w, "could not create user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user registered")
}
// ...existing code...
