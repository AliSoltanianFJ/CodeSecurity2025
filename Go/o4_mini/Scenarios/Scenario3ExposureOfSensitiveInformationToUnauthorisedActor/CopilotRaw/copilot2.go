package main

import (
    "database/sql"
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
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    var storedPassword string
    err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&storedPassword)
    if err == sql.ErrNoRows || err == sql.ErrNoRows {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // NOTE: In production compare hashed passwords (e.g., bcrypt). This example compares plain text.
    if storedPassword != password {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
