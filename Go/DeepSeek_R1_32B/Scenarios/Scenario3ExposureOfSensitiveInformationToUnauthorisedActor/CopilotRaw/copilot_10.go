package main

import (
    "database/sql"
    "encoding/json"
    "net/http"

    "github.com/go-sql-driver/mysql"
)

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

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

    err = db.Ping()
    if err != nil {
        http.Error(w, "Database connection failed", http.StatusInternalServerError)
        return
    }

    query := `SELECT password FROM users WHERE username=?`
    row := db.QueryRow(query, username)

    var storedPassword string
    err = row.Scan(&storedPassword)
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
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteLaxMode,
    })

    w.WriteHeader(http.StatusOK)
}