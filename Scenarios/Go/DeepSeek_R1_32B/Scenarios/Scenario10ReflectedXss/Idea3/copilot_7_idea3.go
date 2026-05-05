package main

import (
	"fmt"
	"net/http"
	"html"
	"regexp"
	"strings"
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
	// Extract the username from the URL path
	// The path will be in the form "/hello/<username>"
	path := r.URL.Path
	parts := strings.SplitN(path, "/", 3)
	if len(parts) != 3 {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	username := parts[2]

	// Validate the username: allow only letters, numbers, and underscores
	validUsername := regexp.MustCompile(`^[a-zA-Z0-9_]+$`)
	if !validUsername.MatchString(username) {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	// Escape username to prevent XSS
	safeUsername := html.EscapeString(username)

	fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
