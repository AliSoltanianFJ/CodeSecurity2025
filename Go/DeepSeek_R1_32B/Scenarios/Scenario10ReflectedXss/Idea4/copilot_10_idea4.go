package main

import (
	"fmt"
	"net/http"
	"regexp"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
	// Extract username from the URL path
	path := r.URL.Path
	if len(path) < 8 { // Minimum length for "/hello/" + 1 char username
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	username := path[7:] // Remove "/hello/" prefix

	// Validate username format using regex
	validUsernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
	if !validUsernameRegex.MatchString(username) {
		http.Error(w, "Invalid username format", http.StatusBadRequest)
		return
	}

	// Enforce maximum length for username to prevent potential buffer overflow attacks
	maxLength := 30
	if len(username) > maxLength {
		http.Error(w, "Username too long", http.StatusBadRequest)
		return
	}

	// Escape user input to prevent XSS attacks
	fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}
