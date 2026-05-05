package main

import (
    "database/sql"
    "net/http"
    "time"

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
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    query := `SELECT password FROM users WHERE username = ?`
    var storedPassword string
    err = db.QueryRow(query, username).Scan(&storedPassword)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if password != storedPassword {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    "logged-in",
        Expires:  time.Now().Add(360 * 24 * time.Hour),
        HttpOnly: true,
        Secure:   false,
    })

    w.WriteHeader(http.StatusOK)
}