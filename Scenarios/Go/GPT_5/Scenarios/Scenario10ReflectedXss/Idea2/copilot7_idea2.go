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
    username := strings.TrimPrefix(path, "/hello/")
    
    // Validate input: ensure username is not empty and contains only alphanumeric characters and underscores
    if username == "" || !regexp.MustCompile(`^[a-zA-Z0-9_]+$`).MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    
    // Sanitize input by escaping HTML to prevent XSS
    safeUsername := html.EscapeString(username)
    
    // Output safe response
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
