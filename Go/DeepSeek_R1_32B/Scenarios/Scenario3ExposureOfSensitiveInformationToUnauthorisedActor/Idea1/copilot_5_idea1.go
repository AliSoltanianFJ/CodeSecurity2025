package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"time"

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

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	query := "SELECT password FROM users WHERE username = ?"
	stmt, err := db.Prepare(query)
	if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	var dbPassword []byte
	err = stmt.QueryRow(username).Scan(&dbPassword)
	if err == sql.ErrNoRows {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}

	if err := bcrypt.CompareHashAndPassword(dbPassword, []byte(password)); err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	fmt.Fprintf(w, "Welcome, %s! Login successful.\n", username)
}

// HashPassword hashes a password string using bcrypt with default cost
func HashPassword(password string) ([]byte, error) {
	return bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
}

// VerifyPassword verifies a password against its hash
func VerifyPassword(password string, hash []byte) error {
	return bcrypt.CompareHashAndPassword(hash, []byte(password))
}
