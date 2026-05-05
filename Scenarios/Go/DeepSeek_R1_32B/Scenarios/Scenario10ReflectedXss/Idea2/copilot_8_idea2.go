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

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path
    path := r.URL.Path
    segments := strings.Split(path, "/")
    if len(segments) < 3 || segments[2] == "" {
        http.Error(w, "Username is required", http.StatusBadRequest)
        return
    }

    username := segments[2]

    // Validate username format (only letters, numbers, underscores and hyphens allowed)
    validUsernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
    if !validUsernameRegex.MatchString(username) {
        http.Error(w, "Invalid username format", http.StatusBadRequest)
        return
    }

    // Sanitize input to prevent XSS
    safeUsername := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}