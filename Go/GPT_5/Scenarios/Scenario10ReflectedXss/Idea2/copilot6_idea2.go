package main

import (
    "fmt"
    "html"
    "net/http"
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
    
    // Validate input: ensure username is not empty and contains only alphanumeric characters and underscores
    if username == "" || !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    
    // Sanitize and escape to prevent XSS
    escapedUsername := html.EscapeString(username)
    
    // Output response
    fmt.Fprintf(w, "<p>Hello, %s!</p>", escapedUsername)
}

// Helper function to validate username (alphanumeric and underscores only)
func isValidUsername(username string) bool {
    for _, r := range username {
        if !((r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '_') {
            return false
        }
    }
    return true
}
