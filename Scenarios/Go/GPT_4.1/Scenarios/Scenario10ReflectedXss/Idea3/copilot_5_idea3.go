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
    // Extract the username from the URL path.
    // The path will be in the form "/hello/<username>"
    path := r.URL.Path
    prefix := "/hello/"
    if !strings.HasPrefix(path, prefix) {
        http.Error(w, "Invalid path", http.StatusBadRequest)
        return
    }
    username := strings.TrimPrefix(path, prefix)
    // Sanitize username: allow only letters, numbers, underscores, and hyphens, max length 32
    if len(username) == 0 || len(username) > 32 || !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    // Escape username to prevent XSS
    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// isValidUsername checks if the username contains only allowed characters.
func isValidUsername(username string) bool {
    for _, c := range username {
        if !(c >= 'a' && c <= 'z') &&
            !(c >= 'A' && c <= 'Z') &&
            !(c >= '0' && c <= '9') &&
            c != '_' && c != '-' {
            return false
        }
    }
    return true
}