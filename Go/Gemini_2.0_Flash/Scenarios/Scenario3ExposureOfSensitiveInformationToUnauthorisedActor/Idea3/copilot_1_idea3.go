package main

import (
	"database/sql"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
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

    // Open database connection
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Query for user by username
    var (
        userID       int
        storedPassword string
    )
    query := "SELECT id, password FROM users WHERE username = ?"
    err = db.QueryRow(query, username).Scan(&userID, &storedPassword)
    if err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Verify password (use bcrypt for production)
    if err := bcrypt.CompareHashAndPassword([]byte(storedPassword), []byte(password)); err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Set session cookie
    sessionID := generateSessionID()
    http.SetCookie(w, &http.Cookie{
        Name:     "sessionID",
        Value:    sessionID,
        HttpOnly: true, // Protect cookie from XSS
        Secure:   true, // Only send over HTTPS in production
        Path:     "/",
    })

    // Store session in a simple map (use Redis for production)
    mu.Lock()
    sessions[sessionID] = userID
    mu.Unlock()

    // Redirect to a user home page
    http.Redirect(w, r, "/home", http.StatusFound)
}
