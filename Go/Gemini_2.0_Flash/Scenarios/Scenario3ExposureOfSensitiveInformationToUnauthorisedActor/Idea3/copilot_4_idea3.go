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
/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form values
    username := r.FormValue("username")
    password := r.FormValue("password")

    // Database connection
    db, err := sql.Open("mysql", "user:password@tcp(db:3306)/db")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Look up the user by username
    var dbPassword string
    if err := db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&dbPassword); err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Verify password using bcrypt
    if err := bcrypt.CompareHashAndPassword([]byte(dbPassword), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Generate JWT token
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
        "username": username,
        "exp":      time.Now().Add(time.Hour * 24).Unix(), // Token valid for 24 hours
    })
    tokenString, err := token.SignedString([]byte("secretkey")) // Use a secure key in production
    if err != nil {
        http.Error(w, "Token generation error", http.StatusInternalServerError)
        return
    }

    // Return token as JSON
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"token": tokenString})
}
