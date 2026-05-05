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

// ...existing code...
func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
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
        // Log server-side, return generic error to client to avoid leaking details
        log.Printf("db open error: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    // Use a prepared statement to avoid SQL injection
    stmt, err := db.Prepare("SELECT password_hash FROM users WHERE username = ?")
    if err != nil {
        log.Printf("prepare statement error: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var passwordHash string
    err = stmt.QueryRow(username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        // Don't reveal whether the username exists
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("query error: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password (assuming passwords are stored with bcrypt)
    if err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)); err != nil {
        // Wrong password
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Create a new session id and set it as a secure, HttpOnly cookie.
    // In a real app you would store the session server-side (DB, cache) and associate it with the user.
    sessionIDBytes := make([]byte, 32)
    if _, err := rand.Read(sessionIDBytes); err != nil {
        log.Printf("session id generation error: %v", err)
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
    sessionID := hex.EncodeToString(sessionIDBytes)

    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set to true in production with HTTPS
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Logged in"))
}
// ...existing code...
