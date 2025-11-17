package main

import (
    "fmt"
    "html"
    "net/http"
    "strings"
)

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Split the URL path into segments
    pathSegments := r.URL.Path[1:] // Remove leading slash
    pathSegments = pathSegments[:len(pathSegments)-1] // Remove trailing slash
    usernameParts := []string{}
    if pathSegments != "" {
        usernameParts = r.URL.Path[7:].split('/')
    }

    // Validate input and prevent buffer overflow by checking length and characters
    if len(usernameParts) != 2 || len(usernameParts[1]) > 50 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Sanitize the username to prevent XSS attacks
    sanitizedUsername := html.EscapeString(usernameParts[1])

    // Generate a safe response
    response := fmt.Sprintf("<p>Hello, %s!</p>", sanitizedUsername)
    fmt.Fprint(w, response)
}