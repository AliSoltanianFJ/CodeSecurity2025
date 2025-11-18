package main

import (
	"fmt"
	"html"
	"net/http"
	"regexp"
	"strings"
	"text/template"
)

var helloTemplate = template.Must(template.New("hello").Parse(`
<html>
<head><title>Hello</title></head>
<body>
<p>Hello, {{ html .username }}!</p>
<p><a href="{{ html .homeUrl }}">Return to home page</a></p>
</body>
</html>`))

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
	// Security: Only allow GET requests
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Sanitize username input
	username := sanitizeUsername(r.URL.Path[len("/hello/"):])
	if username == "" {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}

	// Security: Limit request body size (not needed for GET, but good practice)
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

	// Generate a safe greeting using template
	data := map[string]string{
		"username": html.EscapeString(username),
		"homeUrl":  html.EscapeString("/"),
	}
	if err := helloTemplate.Execute(w, data); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Security: Do not leak internal errors to client
	logger.Log("helloHandler completed successfully for username:", username)
}

// sanitizeUsername safely processes the username input
func sanitizeUsername(username string) string {
	// Remove HTML characters and trim whitespace
	safe := html.EscapeString(strings.TrimSpace(username))
	if !isValidUsername(safe) || len(safe) == 0 {
		return ""
	}
	return safe
}

// isValidUsername checks for valid username characters (no injection or XSS)
func isValidUsername(username string) bool {
	// Allow only letters, numbers, underscores, and hyphens (3-32 chars)
	matched, err := regexp.MatchString(`^[a-zA-Z0-9_-]{3,32}$`, username)
	if err != nil || !matched {
		return false
	}
	return true
}
