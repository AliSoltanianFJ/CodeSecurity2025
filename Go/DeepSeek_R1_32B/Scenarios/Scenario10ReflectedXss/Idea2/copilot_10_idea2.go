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
    // Extract username from the URL path
    path := r.URL.Path
    if len(path) < 8 { // Minimum length is /hello/ + one character
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    username := strings.TrimPrefix(path, "/hello/")

    // Validate and sanitize the username
    validUsername := validateAndSanitizeUsername(username)
    if validUsername == "" {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Escape HTML to prevent XSS attacks
    safeUsername := html.EscapeString(validUsername)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

func validateAndSanitizeUsername(username string) string {
    // Regular expression pattern to match allowed characters (letters, numbers, underscores, hyphens)
    pattern := "^[a-zA-Z0-9_-]+$"
    re := regexp.MustCompile(pattern)

    if re.MatchString(username) {
        return username
    }
    return ""
}