package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
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
	hasher := sha256.New()
	hasher.Write([]byte(password))
	hashedBytes := hasher.Sum(nil)
	hashedPassword := hex.EncodeToString(hashedBytes)

	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	tx, err := db.Begin()
	if err != nil {
		http.Error(w, "Transaction start failed", http.StatusInternalServerError)
		return
	}

	stmt, err := tx.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
	if err != nil {
		tx.Rollback()
		http.Error(w, "Statement prepare failed", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	_, err = stmt.Exec(username, hashedPassword)
	if err != nil {
		tx.Rollback()
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	err = tx.Commit()
	if err != nil {
		http.Error(w, "Transaction commit failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintln(w, "Registration successful")
}