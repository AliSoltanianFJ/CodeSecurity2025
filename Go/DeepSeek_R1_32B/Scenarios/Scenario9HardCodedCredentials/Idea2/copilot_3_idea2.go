package main

import (
	"fmt"
	"html"
	"net/http"
	"golang.org/x/crypto/bcrypt"
)

var moderators = map[string]string{
	// Example: "username": "bcrypt_hashed_password"
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	username := html.EscapeString(r.FormValue("username"))
	password := r.FormValue("password")

	if username == "" || password == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, "Username and password are required")
		return
	}

	modPassword, exists := moderators[username]
	if !exists {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintln(w, "Invalid credentials")
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(modPassword), []byte(password)); err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprintln(w, "Invalid credentials")
		return
	}

	fmt.Fprintln(w, "true")
}
