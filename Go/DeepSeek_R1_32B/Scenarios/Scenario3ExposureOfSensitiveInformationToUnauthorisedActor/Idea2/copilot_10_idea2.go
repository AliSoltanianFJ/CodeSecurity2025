package main

import (
    "database/sql"
    "encoding/gob"
    "fmt"
    "net/http"
    
    "github.com/dgrijalva/jwt-go"
    "github.com/go-sql-driver/mysql"
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

    // Validate inputs
    if username == "" || password == "" {
        http.Error(w, "Username and password are required", http.StatusBadRequest)
        return
    }

    // Sanitize input to prevent SQL injection
    sanitizedUsername := sql.EscapeIdentifier(username)

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var hashedPassword string
    err = db.QueryRow("SELECT password FROM users WHERE username=?", sanitizedUsername).Scan(&hashedPassword)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "Database error occurred", http.StatusInternalServerError)
        return
    }

    // Compare passwords securely using bcrypt
    err = bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
    if err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate JWT token for authentication
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, &jwt.Claims{
        Subject: username,
    })

    tokenString, err := token.SignedString([]byte("your-secure-key"))
    if err != nil {
        http.Error(w, "Token generation failed", http.StatusInternalServerError)
        return
    }

    // Set cookie with secure flags
    cookie := http.Cookie{
        Name:     "auth",
        Value:    tokenString,
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteLaxMode,
    }
    http.SetCookie(w, &cookie)

    http.Redirect(w, r, "/", http.StatusSeeOther)
}

