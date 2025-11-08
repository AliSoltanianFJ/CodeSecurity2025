package main

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"
	"encoding/hex"
	_ "github.com/go-sql-driver/mysql"
	"log"
)

func main() {
	http.HandleFunc("/register", registerHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
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

	hashedPassword := sha256Hash(password)

	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		log.Println("Database connection error:", err)
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	query := "INSERT INTO users (username, password) VALUES (?, ?)"
	result, err := db.Exec(query, username, hashedPassword)
	if err != nil {
		log.Println("Database insertion error:", err)
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		log.Println("Error getting rows affected:", err)
		http.Error(w, "Error during registration", http.StatusInternalServerError)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintln(w, "Registration successful")
	} else {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
	}
}

func sha256Hash(password string) string {
	hasher := sha256.New()
	hasher.Write([]byte(password))
	hashedBytes := hasher.Sum(nil)
	return hex.EncodeToString(hashedBytes)
}