package main

import (
	"fmt"
	"net/http"
)

// Mock database (replace with a real database in production)
var moderators = map[string]string{
	"modUser": "hashedPassword", // Store hashed passwords
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	storedPassword, ok := moderators[username]
	if ok && password == storedPassword { // Replace with proper password hashing and comparison
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}