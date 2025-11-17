package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/sessions"
	_ "github.com/go-sql-driver/mysql"
)

var session *sessions.Session
var store = sessions.NewCookieStore([]byte("your-secure-key"))

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if len(username) == 0 || len(password) == 0 {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if len(username) > 50 || len(password) > 50 {
		http.Error(w, "Invalid input length", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		fmt.Printf("Error opening database: %v\n", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	query := `SELECT username FROM users WHERE username = ? AND password = ? LIMIT 1`
	stmt, err := db.Prepare(query)
	if err != nil {
		fmt.Printf("Error preparing statement: %v\n", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	var user string
	rows, err := stmt.Query(username, password)
	if err != nil {
		fmt.Printf("Error executing query: %v\n", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	if !rows.Next() {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	session.Options = &sessions.Options{
		MaxAge:   3600,
		HttpOnly: true,
		Secure:   true,
		SameSite: sessions.SameSiteStrictMode,
	}
	session.Name = "session"

	session, err = store.Get(r, session.Name)
	if err != nil {
		fmt.Printf("Error creating session: %v\n", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	session.Values["username"] = username
	err = session.Save(r, w)
	if err != nil {
		fmt.Printf("Error saving session: %v\n", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
}
