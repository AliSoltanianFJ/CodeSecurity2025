package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "html"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)
    passwordRegex = regexp.MustCompile(`^[\S]{8,64}$`)
)

type loginResponse struct {
    Message string `json:"message"`
}

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

    // Parse form safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := html.EscapeString(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation
    if !usernameRegex.MatchString(username) || !passwordRegex.MatchString(password) {
        http.Error(w, "Invalid username or password format", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use context with timeout to prevent resource exhaustion
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    var hashedPassword string
    err = db.QueryRowContext(ctx, "SELECT password FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        // Do not reveal if user exists
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Compare password securely
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // On success, respond with a generic message (no sensitive info)
    resp := loginResponse{Message: "Login successful"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}
