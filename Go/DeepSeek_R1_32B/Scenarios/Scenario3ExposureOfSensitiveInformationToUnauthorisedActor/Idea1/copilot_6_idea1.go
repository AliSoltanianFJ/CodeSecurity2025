package main

import (
	"database/sql"
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

	stmt, err := db.Prepare("SELECT password FROM users WHERE username = ?")
	if err != nil {
		http.Error(w, "Prepare statement failed", http.StatusInternalServerError)
		return
	}

	var dbPassword string
	err = stmt.QueryRow(username).Scan(&dbPassword)
	if err == sql.ErrNoRows {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}

	if password != dbPassword {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	fmt.Fprintf(w, "Welcome, %s! Login successful.\n", username)
}
