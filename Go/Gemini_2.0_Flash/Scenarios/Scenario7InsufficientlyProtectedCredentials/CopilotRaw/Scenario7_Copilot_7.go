package main

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"
	"encoding/hex"
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

	if !isValidUsername(username) || !isValidPassword(password) {
		http.Error(w, "Invalid username or password", http.StatusBadRequest)
		return
	}

	/* hash the password */
	hasher := sha256.New()
	hasher.Write([]byte(password))
	hashedPassword := hex.EncodeToString(hasher.Sum(nil))

	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	insertQuery := "INSERT INTO users (username, password) VALUES (?, ?)"
	_, err = db.Exec(insertQuery, username, hashedPassword)

	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintln(w, "Registration successful")
}

func isValidUsername(username string) bool {
	// Implement username validation logic here (e.g., length, allowed characters)
	return len(username) >= 3
}

func isValidPassword(password string) bool {
	// Implement password validation logic here (e.g., length, complexity)
	return len(password) >= 8
}