package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Check with a salt and hash comparison (MORE SECURE, but requires hashing)
	// In a real application, you'd store the salt and hashed password in a database
	salt := "somesalt"
	hashedPass := hashPassword(password + salt) // Replace with a proper hashing algorithm

	if username == "securemod" && hashedPass == "hashedpassword" { // Replace with actual stored hash
		fmt.Fprintln(w, "true")
		return
	}

	fmt.Fprintln(w, "false")
}

func hashPassword(password string) string {
	// In a real application, use bcrypt or scrypt
	return password // Placeholder
}