package main

import (
	"fmt"
	"net/http"
	"regexp"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Regular expression for input validation
	validInput := regexp.MustCompile(`^[a-zA-Z0-9]+$`)
	if !validInput.MatchString(username) || !validInput.MatchString(password) {
		http.Error(w, "Invalid username or password format", http.StatusBadRequest)
		return
	}

	if username == "modUser" && password == "securePassword123" {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}