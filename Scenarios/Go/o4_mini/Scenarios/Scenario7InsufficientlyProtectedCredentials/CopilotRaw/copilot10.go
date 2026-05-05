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
    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    /* hash the password */
    hash := sha256.Sum256([]byte(password))
    hashedPassword := fmt.Sprintf("%x", hash[:])

    // NOTE: Replace DSN with your actual DB credentials and database name.
    dsn := "root:password@tcp(127.0.0.1:3306)/testdb"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        http.Error(w, "database unreachable", http.StatusInternalServerError)
        return
    }

    stmt, err := db.Prepare("INSERT INTO users(username, password_hash) VALUES(?, ?)")
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    if _, err := stmt.Exec(username, hashedPassword); err != nil {
        http.Error(w, "could not create user", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user registered")
}
// ...existing code...
