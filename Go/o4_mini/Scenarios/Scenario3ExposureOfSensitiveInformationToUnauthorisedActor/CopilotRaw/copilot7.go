package main

import (
    "database/sql"
    "crypto/subtle"
    "log"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a prepared statement to avoid injection
    stmt, err := db.Prepare("SELECT password FROM users WHERE username = ?")
    if err != nil {
        log.Println("prepare:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var storedPassword string
    err = stmt.QueryRow(username).Scan(&storedPassword)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Println("query:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Constant-time comparison to avoid timing attacks
    if subtle.ConstantTimeCompare([]byte(storedPassword), []byte(password)) != 1 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
