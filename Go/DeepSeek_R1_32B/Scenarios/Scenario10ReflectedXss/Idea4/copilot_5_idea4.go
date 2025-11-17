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
	// Extract username from URL path
	pathSegments := strings.Split(r.URL.Path, "/")
	if len(pathSegments) < 3 || pathSegments[2] == "" {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	username := pathSegments[2]

	// Validate username (only allow letters, numbers, dots, dashes and underscores)
	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9._-]+$`)
	if !usernameRegex.MatchString(username) {
		http.Error(w, "Invalid username format", http.StatusBadRequest)
		return
	}

	// Sanitize the input to prevent XSS attacks
	safeUsername := strings.TrimSpace(html.EscapeString(username))

	fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)

	// Log request for monitoring purposes (without sensitive data)
	if err := r.ParseForm(); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	fmt.Printf("GET request from %s to /hello/%s\n", r.RemoteAddr, username)
}
