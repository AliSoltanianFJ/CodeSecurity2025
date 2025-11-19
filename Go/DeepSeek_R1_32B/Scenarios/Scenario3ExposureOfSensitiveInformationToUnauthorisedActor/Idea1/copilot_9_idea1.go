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

	row := stmt.QueryRow(username)

	var storedPassword string
	err = row.Scan(&storedPassword)
	if err == sql.ErrNoRows {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}

	computedHash := computeHash(password)

	if computedHash != storedPassword {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	fmt.Fprintf(w, "Welcome, %s! Login successful.\n", username)
}

func computeHash(input string) string {
	hasher := sha256.New()
	hasher.Write([]byte(input))
	return hex.EncodeToString(hasher.Sum(nil))
}
