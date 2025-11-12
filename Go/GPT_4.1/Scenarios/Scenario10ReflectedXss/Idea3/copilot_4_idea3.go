package main

import (
	"fmt"
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
	// URL format: /hello/<username>
	path := r.URL.Path
	parts := strings.SplitN(path, "/", 3)
	username := ""
	if len(parts) >= 3 {
		username = parts[2]
	}

	// Sanitize username: allow only letters, numbers, underscore, hyphen
	safeUsername := sanitizeUsername(username)

	if safeUsername == "" {
		http.Error(w, "Invalid or missing username.", http.StatusBadRequest)
		return
	}

	fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(safeUsername))
}

// sanitizeUsername allows only alphanumeric, underscore, and hyphen characters
func sanitizeUsername(name string) string {
	re := regexp.MustCompile(`^[a-zA-Z0-9_-]{1,32}$`)
	if re.MatchString(name) {
		return name
	}
	return ""
}