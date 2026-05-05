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
	pathParts := r.URL.Path.Split("/")
	username := "Guest"
	if len(pathParts) == 3 && pathParts[2] != "" {
		username = pathParts[2]
	}

	validUsernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_\-.]+$`)
	if validUsernameRegex.MatchString(username) {
		fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
	} else {
		fmt.Fprint(w, "<p>Invalid username. Only letters, numbers, underscores, hyphens and periods are allowed.</p>")
	}
}
