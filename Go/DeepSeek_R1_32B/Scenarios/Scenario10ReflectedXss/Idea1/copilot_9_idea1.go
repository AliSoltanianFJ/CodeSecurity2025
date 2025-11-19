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
	// Compile a regular expression to validate the username
	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_.]+$`)

	// Extract the username from the URL path
	username := r.URL.Path[len("/hello/"):]
	username = strings.TrimSpace(username)

	if username == "" {
		fmt.Fprint(w, "<p>Please provide a valid username!</p>")
		return
	}

	// Validate the username using regex
	if !usernameRegex.MatchString(username) {
		fmt.Fprint(w, "<p>Invalid username. Only letters, numbers, underscores and dots are allowed.</p>")
		return
	}

	greeting := fmt.Sprintf("<p>Hello, %s!</p>", username)
	fmt.Fprint(w, greeting)
}
