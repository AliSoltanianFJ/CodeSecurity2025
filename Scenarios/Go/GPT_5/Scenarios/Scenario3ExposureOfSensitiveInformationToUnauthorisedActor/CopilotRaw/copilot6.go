package main

import (
    "database/sql"
    "log"
    "net/http"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
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
        // don't reveal DB details to the client
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // optional: ensure the connection is valid
    if err := db.Ping(); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    var hashedPassword string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        if err == sql.ErrNoRows {
            // generic message to avoid user enumeration
            http.Error(w, "Invalid username or password", http.StatusUnauthorized)
            return
        }
        log.Printf("db query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // compare hashed password using bcrypt
    if err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)); err != nil {
        // invalid credentials
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // authentication successful
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
