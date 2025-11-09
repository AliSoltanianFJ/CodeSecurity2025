// ...existing code...
package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)
// ...existing code...
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

    var pwHash string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&pwHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // generate a simple session token (store server-side in real apps)
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := hex.EncodeToString(tokenBytes)

    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set to true in production with HTTPS
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
