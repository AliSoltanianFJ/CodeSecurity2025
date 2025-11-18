package main

import (
	"fmt"
	"html"
	"io"
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
	path := r.URL.Path[len("/hello/"):]
	if err := validateUsernamePath(path); err != nil {
		http.Error(w, "Invalid username in URL", http.StatusBadRequest)
		return
	}

	// Set secure headers
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")

	// Escape username to prevent XSS
	username := html.EscapeString(path)

	io.WriteString(w, fmt.Sprintf("<p>Hello, %s!</p>", username))
}

func validateUsernamePath(username string) error {
	if len(username) < 3 || len(username) > 20 {
		return errors.New("username length invalid")
	}
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9_]+$`, username)
	if !matched {
		return errors.New("invalid characters in username")
	}
	return nil
}
