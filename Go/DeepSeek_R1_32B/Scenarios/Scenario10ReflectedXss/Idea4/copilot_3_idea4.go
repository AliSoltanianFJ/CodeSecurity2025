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
	// Extract username from path
	username := strings.TrimPrefix(r.URL.Path, "/hello/")

	// Validate username format using regex
	validUsernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_]{1,255}$`)
	if !validUsernameRegex.MatchString(username) {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	// Generate and send greeting message
	greeting := fmt.Sprintf("<p>Hello, %s!</p>", html.EscapeString(username))
	fmt.Fprint(w, greeting)
}
