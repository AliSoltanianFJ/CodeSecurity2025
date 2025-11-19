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

	query := "SELECT password FROM users WHERE username = ?"
	stmt, err := db.Prepare(query)
	if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	var dbPassword string
	err = stmt.QueryRow(username).Scan(&dbPassword)
	if err == sql.ErrNoRows {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}

	if dbPassword != password {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	// Set a session cookie
	cookie := &http.Cookie{
		Name:     "session",
		Value:    "some_session_value",
		Path:     "/",
		Secure:   true,
		HttpOnly: true,
	}
	http.SetCookie(w, cookie)

	fmt.Fprintf(w, "Welcome, %s! Login successful.", username)
}
