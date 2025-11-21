package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/base64"
    "net/http"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
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

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    var passwordHash string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    // generate a session token and set as an HttpOnly cookie
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    token := base64.URLEncoding.EncodeToString(b)

    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set true if using HTTPS
        Expires:  time.Now().Add(24 * time.Hour),
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
