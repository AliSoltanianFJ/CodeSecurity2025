package main

import (
	"fmt"
	"net/http"
	"strings"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Input validation: Check for empty or excessively long inputs
	if len(username) == 0 || len(password) == 0 || len(username) > 100 || len(password) > 100 {
		http.Error(w, "Invalid username or password", http.StatusBadRequest)
		return
	}

	// Hardcoded credentials (for demonstration purposes only - NEVER in production!)
	if username == "modUser" && password == "securePassword123" {
		fmt.Fprint(w, "true")
		return
	}

	fmt.Fprint(w, "false")
}