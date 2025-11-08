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

	// Placeholder for database check (replace with actual DB interaction)
	isMod := checkUserInDatabase(username, password)
	if isMod {
		fmt.Fprintln(w, "true")
	} else {
		fmt.Fprintln(w, "false")
	}
}

func checkUserInDatabase(username, password string) bool {
	// In a real application, query a database here
	// This is just a stub
	return false
}