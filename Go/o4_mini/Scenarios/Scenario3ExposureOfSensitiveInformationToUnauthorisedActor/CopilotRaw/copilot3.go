package main

import (
    "database/sql"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

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
    // ...existing code...
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err = db.Ping(); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Use a parameterised query to avoid SQL injection.
    stmt, err := db.Prepare("SELECT COUNT(1) FROM users WHERE username = ? AND password = ?")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var count int
    err = stmt.QueryRow(username, password).Scan(&count)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if count == 1 {
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("Login successful"))
        return
    }

    http.Error(w, "Invalid credentials", http.StatusUnauthorized)
}
