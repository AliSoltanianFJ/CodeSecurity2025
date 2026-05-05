package main

import (
	"fmt"
	"html"
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
	segments := r.URL.Path.Split("/")
	usernameSegment := segments[2]

	// Trim any trailing slash
	if usernameSegment == "" && len(segments) > 3 {
		usernameSegment = segments[3]
	}

	username := usernameSegment

	// Input validation
	maxUsernameLength := 30
	if username == "" {
		http.Error(w, "Missing username", http.StatusBadRequest)
		return
	}
	if len(username) > maxUsernameLength {
		http.Error(w, "Username too long", http.StatusBadRequest)
		return
	}

	// Sanitize and validate the username using regex
	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
	if !usernameRegex.MatchString(username) {
		http.Error(w, "Invalid username characters", http.StatusBadRequest)
		return
	}

	safeUsername := html.EscapeString(username)

	fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
