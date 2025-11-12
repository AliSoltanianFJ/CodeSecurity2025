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

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    username = strings.TrimSpace(username)

    // Validate username: only allow letters, numbers, underscores, and hyphens (limit length)
    if !isValidUsername(username) {
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }

    // Escape output to prevent XSS
    safeUsername := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// isValidUsername checks if the username contains only allowed characters and is a reasonable length
func isValidUsername(username string) bool {
    if len(username) < 1 || len(username) > 32 {
        return false
    }
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
    return validUsername.MatchString(username)
}