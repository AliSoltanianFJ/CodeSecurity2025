package main

import (
    "fmt"
    "html"
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

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path
    path := r.URL.Path
    if !strings.HasPrefix(path, "/hello/") {
        http.Error(w, "Invalid path", http.StatusBadRequest)
        return
    }
    username := strings.TrimPrefix(path, "/hello/")
    
    // Validate input: ensure username is not empty, alphanumeric only, and reasonable length
    if username == "" {
        http.Error(w, "Username is required", http.StatusBadRequest)
        return
    }
    if len(username) > 50 {
        http.Error(w, "Username too long", http.StatusBadRequest)
        return
    }
    // Use regex to allow only alphanumeric characters and underscores
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_]+$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username format", http.StatusBadRequest)
        return
    }
    
    // Sanitize and escape to prevent XSS
    safeUsername := html.EscapeString(username)
    
    // Output HTML response
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
