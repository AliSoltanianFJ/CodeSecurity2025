package main

import (
    "database/sql"
    "net/http"
    "golang.org/x/crypto/bcrypt"
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

    query := "SELECT password FROM users WHERE username = ?"
    rows, err := db.Query(query, username)
    if err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    defer rows.Close()
    var hashedPassword []byte
    if !rows.Next() {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    err = rows.Scan(&hashedPassword)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    err = bcrypt.CompareHashAndPassword(hashedPassword, []byte(password))
    if err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Set session cookie or token here as needed
    http.SetCookie(w, &http.Cookie{
        Name:   "session",
        Value:  "some_session_value",
        Path:   "/",
        Secure: false,
    })

    w.WriteHeader(http.StatusOK)
}