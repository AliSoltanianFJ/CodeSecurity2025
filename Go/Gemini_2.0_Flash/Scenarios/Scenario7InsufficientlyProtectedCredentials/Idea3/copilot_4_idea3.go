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
    /* hash the password */
    hashedPassword := hashPassword(password)

    /* open database connection */
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/users")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    /* insert user into database */
    _, err = db.Exec("INSERT INTO users (username, password) VALUES (?, ?)", username, hashedPassword)
    if err != nil {
        http.Error(w, "Registration failed", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Registration successful!\n")
}

/* hashPassword hashes the password using bcrypt. */
func hashPassword(password string) string {
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        panic(err) // bcrypt errors are usually fatal
    }
    return string(hashed)
}
