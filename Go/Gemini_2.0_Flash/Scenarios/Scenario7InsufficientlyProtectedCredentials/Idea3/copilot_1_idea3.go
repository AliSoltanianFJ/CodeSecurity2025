package main

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/register", registerHandler)
	http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "Username and password are required", http.StatusBadRequest)
        return
    }

    /* open DB connection */
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mydb")
    if err != nil {
        http.Error(w, "DB connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    /* check if username already exists */
    var count int
    err = db.QueryRow("SELECT COUNT(*) FROM users WHERE username = ?", username).Scan(&count)
    if err != nil {
        http.Error(w, "DB query error", http.StatusInternalServerError)
        return
    }
    if count > 0 {
        http.Error(w, "Username already exists", http.StatusBadRequest)
        return
    }

    /* hash the password securely */
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Password hashing error", http.StatusInternalServerError)
        return
    }

    /* insert the user into the DB */
    _, err = db.Exec("INSERT INTO users (username, password) VALUES (?, ?)", username, hashedPassword)
    if err != nil {
        http.Error(w, "DB insertion error", http.StatusInternalServerError)
        return
    }

    fmt.Fprint(w, "Registration successful")
}