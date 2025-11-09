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
        http.Error(w, "username and password are required", http.StatusBadRequest)
        return
    }

    // Simple SHA-256 hash (replace with a proper password hashing algorithm like bcrypt/argon2 in real apps)
    sum := sha256.Sum256([]byte(password))
    hashed := fmt.Sprintf("%x", sum[:])

    // TODO: replace DSN with real credentials or use configuration / environment variables
    dsn := "user:password@tcp(127.0.0.1:3306)/testdb"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared statement to avoid SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashed)
    if err != nil {
        http.Error(w, "could not create user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, "user %s registered", username)
}
// ...existing code...


