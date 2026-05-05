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

	// Check against a simple file (INSECURE, FOR DEMO ONLY)
	if checkCredentialsInFile(username, password) {
		fmt.Fprintln(w, "true")
	} else {
		fmt.Fprintln(w, "false")
	}
}

func checkCredentialsInFile(username, password string) bool {
	// Read from a file (e.g., "mods.txt" with "username:password" format)
	// This is a stub implementation
	return false
}