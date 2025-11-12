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
// loginHandler attempts to log in the user with provided username and password.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "DB connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare query with placeholders to prevent SQL injection
    var storedPassword string
    err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&storedPassword)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        } else {
            http.Error(w, "Internal server error", http.StatusInternalServerError)
        }
        return
    }

    // Verify password using bcrypt
    err = bcrypt.CompareHashAndPassword([]byte(storedPassword), []byte(password))
    if err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Create a token for the user
    token, err := createToken(username)
    if err != nil {
        http.Error(w, "Failed to create token", http.StatusInternalServerError)
        return
    }

    // Return the token in JSON format
    json.NewEncoder(w).Encode(map[string]string{"token": token})
}

// createToken generates a JWT token for the given username.
func createToken(username string) (string, error) {
    // Define token claims
    claims := jwt.MapClaims{
        "username": username,
        "exp":      time.Now().Add(time.Hour * 24).Unix(), // Token valid for 24 hours
    }

    // Sign the token with a secret key
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
    secretKey := []byte("your-secret-key") // Change this in production!
    return token.SignedString(secretKey)
}
