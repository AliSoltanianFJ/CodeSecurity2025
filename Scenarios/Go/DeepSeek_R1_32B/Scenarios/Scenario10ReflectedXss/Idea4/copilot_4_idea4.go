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
	// Extract username from path
	pathSegments := strings.Split(r.URL.Path, "/")
	if len(pathSegments) != 3 || pathSegments[2] == "" {
		http.Error(w, "Invalid request format. Expected: /hello/username", http.StatusBadRequest)
		return
	}

	username := pathSegments[2]

	// Validate username input
	maxUsernameLength := 50
	if len(username) > maxUsernameLength {
		http.Error(w, fmt.Sprintf("Username too long (max %d characters)", maxUsernameLength), http.StatusBadRequest)
		return
	}

	// Sanitize the username to prevent XSS attacks
	safeUsername := strings.TrimSpace(username)
	if safeUsername == "" {
		http.Error(w, "Username cannot be empty", http.StatusBadRequest)
		return
	}

	// Escape HTML special characters in the username
	escaper := strings.NewReplacer(
		"&", "&amp;",
		"<", "&lt;",
		">", "&gt;",
		"\"", "&quot;",
		"'", "&#39;",
	)
	sanitizedUsername := escaper.Replace(safeUsername)

	// Set security headers
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'none'")

	// Construct the response
	response := fmt.Sprintf("<p>Hello, %s!</p>", sanitizedUsername)
	fmt.Fprint(w, response)

	// Secure logging without sensitive information
	if len(safeUsername) > 0 {
		fmt.Printf("Request to /hello endpoint by user: [REDACTED] (method: %s, path: %s)\n", r.Method, r.URL.Path)
	}
}
