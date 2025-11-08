package main

import (
	"fmt"
	"net/http"
	"net/url"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Proper URL decoding to prevent injection attacks
	decodedUsername, err := url.QueryUnescape(username)
	if err != nil {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	decodedPassword, err := url.QueryUnescape(password)
	if err != nil {
		http.Error(w, "Invalid password", http.StatusBadRequest)
		return
	}

	if decodedUsername == "modUser" && decodedPassword == "securePassword123" {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}