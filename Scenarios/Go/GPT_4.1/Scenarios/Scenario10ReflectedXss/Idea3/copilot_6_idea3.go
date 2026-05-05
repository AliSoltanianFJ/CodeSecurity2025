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
    path := r.URL.Path
    parts := strings.Split(path, "/")
    if len(parts) < 3 || parts[2] == "" {
        http.Error(w, "Username not provided", http.StatusBadRequest)
        return
    }
    username := parts[2]

    // Simple input validation: allow only letters, numbers, underscores, hyphens
    if !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Escape username to prevent XSS
    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// isValidUsername checks if the username contains only allowed characters
func isValidUsername(username string) bool {
    for _, c := range username {
        if !(c == '-' || c == '_' ||
            (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9')) {
            return false
        }
    }
    return true
}