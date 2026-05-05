package main

import (
	"context"
	"database/sql"
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/sessions"
	_ "github.com/go-sql-driver/mysql"
)

var sessionStore = sessions.NewCookieStore([]byte("your-secure-key"))

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

	ctx, cancel := context.WithTimeout(r.Context(), time.Second*5)
	defer cancel()

	query := "SELECT password FROM users WHERE username = ? AND password = ?"
	row := db.QueryRowContext(ctx, query, username, password)

	var dbPassword string
	err = row.Scan(&dbPassword)
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	session, err := sessionStore.New(r.Context(), "session-name")
	if err != nil {
		http.Error(w, "Session error", http.StatusInternalServerError)
		return
	}
	session.Values["username"] = username
	err = session.Save(r, w)
	if err != nil {
		http.Error(w, "Session save error", http.StatusInternalServerError)
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     "auth",
		Value:    "logged-in",
		Expires:  time.Now().Add(time.Hour * 24),
		HttpOnly: true,
		Secure:   true,
	})

	fmt.Fprintf(w, "Welcome, %s! Login successful.\n", username)
}
