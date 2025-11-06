package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"strings"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/login", loginHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
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

	username = strings.TrimSpace(username)
	password = strings.TrimSpace(password)

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		log.Println("Database connection error:", err)
		return
	}
	defer db.Close()

	stmt, err := db.Prepare("SELECT username FROM users WHERE username = ? AND password = ?")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		log.Println("Prepare statement error:", err)
		return
	}
	defer stmt.Close()

	var retrievedUsername string
	err = stmt.QueryRow(username, password).Scan(&retrievedUsername)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Invalid credentials", http.StatusUnauthorized)
			return
		}
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		log.Println("Query error:", err)
		return
	}

	fmt.Fprintln(w, "Login successful")
}