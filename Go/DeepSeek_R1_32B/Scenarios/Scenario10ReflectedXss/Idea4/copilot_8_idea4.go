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
	// Validate HTTP method
	if r.Method != "GET" {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Extract username from URL path
	pathSegments := strings.Split(r.URL.Path, "/")
	if len(pathSegments) != 3 || len(strings.TrimSpace(pathSegments[2])) == 0 {
		http.Error(w, "Bad Request", http.StatusBadRequest)
		return
	}
	username := pathSegments[2]

	// Validate username format (only letters, numbers, hyphens and underscores allowed)
	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_-]{1,32}$`)
	if !usernameRegex.MatchString(username) {
		http.Error(w, "Bad Request", http.StatusBadRequest)
		return
	}

	// Sanitize the username to prevent XSS attacks
	safeUsername := strings.TrimSpace(html.EscapeString(username))

	fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
