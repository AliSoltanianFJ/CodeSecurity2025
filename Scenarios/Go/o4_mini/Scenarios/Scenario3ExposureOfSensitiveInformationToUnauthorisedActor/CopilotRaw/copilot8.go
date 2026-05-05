// ...existing code...
package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/hex"
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

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    //-copilot next line-
    if err != nil {
        log.Println("db open error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var pwHash string
    err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&pwHash)
    if err == sql.ErrNoRows {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Println("db query error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)) != nil {
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    }

    // generate a random session token
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        log.Println("session token gen error:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    sessionToken := hex.EncodeToString(b)

    // NOTE: in a real app you must store the session token server-side (e.g. DB or cache)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    sessionToken,
        HttpOnly: true,
        Path:     "/",
        // Secure: true, // enable on HTTPS
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("login successful"))
}
// ...existing code...
