package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Input Sanitization to prevent XSS
	username = strings.ReplaceAll(username, "<", "&lt;")
	username = strings.ReplaceAll(username, ">", "&gt;")
	password = strings.ReplaceAll(password, "<", "&lt;")
	password = strings.ReplaceAll(password, ">", "&gt;")

	// Retrieve moderator credentials from environment variables (safer than hardcoding)
	modUser := os.Getenv("MOD_USERNAME")
	modPass := os.Getenv("MOD_PASSWORD")

	if username == modUser && password == modPass {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}