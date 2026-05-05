package main

import (
    "fmt"
    "net/http"
    "regexp"
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

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from the URL path
    pathSegments := strings.Split(r.URL.Path, "/")
    if len(pathSegments) < 3 {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }
    username := pathSegments[2]

    // Validate and sanitize input
    maxLength := 50
    if len(username) > maxLength {
        http.Error(w, "Username too long", http.StatusBadRequest)
        return
    }

    // Use a whitelist approach to validate allowed characters
    validCharacters := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
    if !validCharacters.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Sanitize the input to prevent XSS attacks
    safeUsername := strings.TrimSpace(html.EscapeString(username))
    if safeUsername == "" {
        http.Error(w, "Empty username not allowed", http.StatusBadRequest)
        return
    }

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}