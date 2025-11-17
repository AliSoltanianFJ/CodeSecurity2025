package main

import (
	"fmt"
	"net/http"
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
	pathSegments := strings.Split(r.URL.Path, "/")
	if len(pathSegments) < 3 || pathSegments[2] == "" {
		http.Error(w, "Bad Request", http.StatusBadRequest)
		return
	}

	username := pathSegments[2]

	// Validate username format (only letters, numbers, underscores and dots allowed)
	validUsernameRegex := regexp.MustCompile(`^[a-zA-Z0-9._]+$`)
	if !validUsernameRegex.MatchString(username) {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	// Sanitize the username to prevent XSS attacks
	safeUsername := strings.TrimSpace(username)

	// Generate a secure greeting message
	greeting := fmt.Sprintf("<p>Hello, %s!</p>", safeUsername)

	fmt.Fprint(w, greeting)
}
