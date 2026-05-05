package main

import (
    "fmt"
    "net/http"
    "regexp"
    "strings"
    "html"
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
    pathSegments := strings.Split(r.URL.Path, "/")
    if len(pathSegments) < 3 || pathSegments[2] == "" {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    username := pathSegments[2]

    // Validate and sanitize the username
    if !isValidUsername(username) {
        http.Error(w, "Invalid characters in username", http.StatusBadRequest)
        return
    }

    safeUsername := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// isValidUsername checks if the username contains only allowed characters
func isValidUsername(username string) bool {
    // Only allow letters, numbers, underscores, hyphens, and periods
    matched, _ := regexp.MatchString(`^[a-zA-Z0-9._-]+$`, username)
    return matched
}