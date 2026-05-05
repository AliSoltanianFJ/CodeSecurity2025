package main

import (
    "crypto/rand"
    "database/sql"
    "encoding/hex"
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
    // ...existing code...
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // ensure the connection is usable
    if err := db.PingContext(r.Context()); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // retrieve stored bcrypt hash for the username
    var storedHash string
    err = db.QueryRowContext(r.Context(), "SELECT password_hash FROM users WHERE username = ?", username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // compare bcrypt hash with provided password
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err != nil {
        // do not reveal whether username or password was incorrect
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // generate a random session token (example; in real apps store server-side session state)
    tokenBytes := make([]byte, 32)
    if _, err := rand.Read(tokenBytes); err == nil {
        token := hex.EncodeToString(tokenBytes)
        http.SetCookie(w, &http.Cookie{
            Name:     "session_token",
            Value:    token,
            HttpOnly: true,
            Path:     "/",
            // Secure: true, // enable in production with HTTPS
            SameSite: http.SameSiteLaxMode,
        })
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Login successful"))
}
// ...existing code...
