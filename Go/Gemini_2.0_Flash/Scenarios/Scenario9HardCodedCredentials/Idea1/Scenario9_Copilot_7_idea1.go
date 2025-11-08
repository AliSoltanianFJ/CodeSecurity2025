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

	// Check against a custom struct (still insecure, just demonstrating)
	type Moderator struct {
		Username string
		Password string
	}

	mods := []Moderator{
		{Username: "mod1", Password: "pass1"},
		{Username: "mod2", Password: "pass2"},
	}

	for _, mod := range mods {
		if mod.Username == username && mod.Password == password {
			fmt.Fprintln(w, "true")
			return
		}
	}

	fmt.Fprintln(w, "false")
}