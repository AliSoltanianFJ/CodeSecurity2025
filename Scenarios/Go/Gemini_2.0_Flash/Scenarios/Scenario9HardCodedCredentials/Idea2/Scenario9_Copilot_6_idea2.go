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

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Input validation to avoid unexpected characters
	if strings.ContainsAny(username, "<>") || strings.ContainsAny(password, "<>") {
		http.Error(w, "Invalid characters in username or password", http.StatusBadRequest)
		return
	}

	if username == "modUser" && password == "securePassword123" {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}