package main

import (
    "fmt"
    "html"
    "net/http"
    "strings"
)

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Split the URL path to extract the username
    pathSegments := strings.Split(r.URL.Path, "/")
    username := pathSegments[2]

    // Validate the input
    if username == "" {
        http.Error(w, "Username cannot be empty", http.StatusBadRequest)
        return
    }

    // Sanitize and validate the username
    if !isSafeUsername(username) {
        http.Error(w, "Invalid username. Only letters and numbers are allowed", http.StatusBadRequest)
        return
    }

    // Escape special characters to prevent XSS attacks
    safeUsername := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// isSafeUsername checks if the username contains only alphanumeric characters
func isSafeUsername(username string) bool {
    for _, c := range username {
        if !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
            return false
        }
    }
    return true
}