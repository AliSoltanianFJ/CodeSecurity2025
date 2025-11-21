// ...existing code...
package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/hex"
    "log"
    "net/http"
    "time"

    _ "github.com/go-sql-driver/mysql"
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
        log.Println("db open error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use a prepared query (QueryRow with placeholder) to avoid SQL injection
    var hashedPassword string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err == sql.ErrNoRows {
        // Do not reveal whether username or password was wrong
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        log.Println("db query error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare supplied password with stored hash
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate a session token
    token, err := generateToken(32)
    if err != nil {
        log.Println("token generation error:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // NOTE: In a real application you must store the token server-side (DB or in-memory store)
    // and associate it with the user. Here we just set a secure cookie for demonstration.
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set to true when running over HTTPS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    })

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}

func generateToken(n int) (string, error) {
    b := make([]byte, n)
    _, err := rand.Read(b)
    if err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
// ...existing code...
