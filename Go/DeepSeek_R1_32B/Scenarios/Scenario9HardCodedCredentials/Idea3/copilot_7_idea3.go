package main

import (
	"crypto/subtle"
	"fmt"
	"net/http"
	"os"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	if username == "" || password == "" {
		http.Error(w, "Username and password required", http.StatusBadRequest)
		return
	}

	expectedUsername := os.Getenv("MOD_USERNAME")
	expectedPassword := os.Getenv("MOD_PASSWORD")

	if expectedUsername == "" || expectedPassword == "" {
		http.Error(w, "Internal error: missing credentials", http.StatusInternalServerError)
		return
	}

	// Constant-time comparison to prevent timing attacks
	usernameMatch := subtle.ConstantTimeByteEq([]byte(username), []byte(expectedUsername))
	passwordMatch := subtle.ConstantTimeByteEq([]byte(password), []byte(expectedPassword))

	if usernameMatch && passwordMatch {
		fmt.Fprintln(w, "true")
	} else {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
	}
}
