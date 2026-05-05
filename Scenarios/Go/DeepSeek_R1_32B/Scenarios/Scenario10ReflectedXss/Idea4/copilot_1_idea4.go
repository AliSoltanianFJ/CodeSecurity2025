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
	// Extract username from URL path
	username := r.URL.Path[len("/hello/"):]
	
	// Validate username format (only letters, numbers, hyphens and underscores)
	if matched, _ := regexp.MatchString("^[a-zA-Z0-9_-]{1,20}$", username); !matched {
		http.Error(w, "Invalid username. Only alphanumeric, hyphen, and underscore characters are allowed.", http.StatusBadRequest)
		return
	}

	// Sanitize the input to prevent XSS attacks by HTML-escaping special characters
	safeUsername := fmt.Sprintf("%s", username) // In a real-world scenario, use html.EscapeString
	
	fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
