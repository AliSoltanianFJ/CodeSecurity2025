package main

import (
	"fmt"
	"net/http"
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
	path := r.URL.Path
	parts := strings.SplitN(path, "/", 3)
	if len(parts) < 3 || !isAlphanumeric(parts[2]) {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	username := html.EscapeString(parts[2])
	fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}

// isAlphanumeric checks if a string contains only alphanumeric characters
func isAlphanumeric(s string) bool {
	for _, r := range s {
		if !(r >= 'a' && r <= 'z') && !(r >= 'A' && r <= 'Z') && !(r >= '0' && r <= '9') {
			return false
		}
	}
	return true
}
