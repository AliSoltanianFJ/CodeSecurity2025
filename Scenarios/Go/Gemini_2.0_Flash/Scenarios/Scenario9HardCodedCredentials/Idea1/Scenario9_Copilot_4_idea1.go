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

	// Simulate checking against an environment variable
	// (Remember to set the environment variable)
	if username == "envmod" && password == "envpass" {
		fmt.Fprintln(w, "true")
		return
	}
	fmt.Fprintln(w, "false")
}