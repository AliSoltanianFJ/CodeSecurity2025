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

	// Simple hardcoded check (INSECURE, FOR DEMO ONLY)
	if username == "mod" && password == "modpass" {
		fmt.Fprintln(w, "true")
		return
	}
	fmt.Fprintln(w, "false")
}