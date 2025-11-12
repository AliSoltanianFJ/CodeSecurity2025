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

    // Get DB credentials from environment variables (Do NOT hardcode)
    dbUser := os.Getenv("DB_USER")
    dbPass := os.Getenv("DB_PASS")
    dbHost := os.Getenv("DB_HOST")
    dbName := os.Getenv("DB_NAME")
    dsn := dbUser + ":" + dbPass + "@tcp(" + dbHost + ")/" + dbName

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "DB connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var hashedPassword string
    err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    io.WriteString(w, "Login successful")
}
