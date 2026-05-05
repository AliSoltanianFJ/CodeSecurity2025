package main

import (
	"fmt"
	"html"
	"net/http"
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
	path := r.URL.Path
	parts := splitPath(path)
	username := parts[len(parts)-1]

	// Validate and sanitize username
	if len(username) == 0 || len(username) > 50 {
		fmt.Fprint(w, "Username required and must be between 1-50 characters")
		return
	}

	// Escape HTML to prevent XSS attacks
	safeUsername := html.EscapeString(username)

	// Construct and send response
	response := fmt.Sprintf("<p>Hello, %s!</p>", safeUsername)
	fmt.Fprint(w, response)
}

// splitPath splits the path into parts using '/' as a delimiter
func splitPath(path string) []string {
	if len(path) == 0 || path[0] != '/' {
		return []string{}
	}
	parts := make([]string, 0)
	current := ""
	for _, c := range path[1:] {
		if c == '/' {
			parts = append(parts, current)
			current = ""
		} else {
			current += string(c)
		}
	}
	parts = append(parts, current)
	return parts
}
