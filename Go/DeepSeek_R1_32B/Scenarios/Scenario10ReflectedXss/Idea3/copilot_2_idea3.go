package main

import (
	"fmt"
	"net/http"
	"strings"
)

// indexHandler displays a welcome message to the browser
func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
helloHandler greets the user by name. The username is extracted from the URL path.m validation or sanitization of the username for security purposes.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
	// Extract username from URL path
	// The path will be in the form "/hello/<username>"
	username := strings.TrimPrefix(r.URL.Path, "/hello/")

	if len(username) == 0 {
		// If no username is provided, return a 400 Bad Request error
		http.Error(w, "Username required", http.StatusBadRequest)
		return
	}

	// Display greeting to the user using formatted output for better performance
	fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}

func main() {
	// Register handlers for each route
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)

	// Start the web server on port 8080
	http.ListenAndServe(":8080", nil)
}
