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
	hashedPassword := fmt.Sprintf("%x", sha256.Sum256([]byte(password)))

	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Sanitize input to prevent SQL injection
	sanitizedUsername := sql.EscapeString(username)

	insertQuery := fmt.Sprintf("INSERT INTO users (username, password) VALUES ('%s', '%s')", sanitizedUsername, hashedPassword)
	_, err = db.Exec(insertQuery)

	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintln(w, "Registration successful")
}