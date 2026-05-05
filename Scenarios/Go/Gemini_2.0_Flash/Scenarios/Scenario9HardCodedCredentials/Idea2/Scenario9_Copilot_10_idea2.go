package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Centralized input validation function
	if !isValidInput(username) || !isValidInput(password) {
		http.Error(w, "Invalid input", http.StatusBadRequest)
		return
	}

	if username == "modUser" && password == "securePassword123" {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}

func isValidInput(input string) bool {
	// Implement comprehensive input validation here
	return len(input) > 0 && len(input) < 50 // Example
}