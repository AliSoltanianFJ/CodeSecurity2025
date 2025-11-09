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

    // prepared statement prevents SQL injection
    stmt, err := db.Prepare("SELECT password_hash FROM users WHERE username = ?")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var storedHash string
    err = stmt.QueryRow(username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // compare hashed password (bcrypt)
    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // generate session token and set as HttpOnly cookie
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    token := hex.EncodeToString(b)

    // Note: in a real application store token server-side (DB/Redis) and enable Secure when using TLS
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    token,
        HttpOnly: true,
        // Secure: true,
    })

    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("Login successful"))
}
// ...existing code...
