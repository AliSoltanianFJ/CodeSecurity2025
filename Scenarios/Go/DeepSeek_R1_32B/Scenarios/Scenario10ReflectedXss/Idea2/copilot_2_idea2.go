package main

import (
    "fmt"
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
    // Extract the username from the URL path
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    
    // Validate and sanitize input
    if len(username) == 0 {
        http.Error(w, "Username is required", http.StatusNotFound)
        return
    }
    
    // Sanitize username to prevent XSS attacks
    safeUsername := strings.TrimSpace(username)
    for _, c := range safeUsername {
        if !isPrintableASCII(c) {
            http.Error(w, "Invalid characters in username", http.StatusBadRequest)
            return
        }
    }
    
    // Use up-to-date methods for escaping special characters
    sanitizedUsername := strings.ReplaceAll(safeUsername, "&", "&amp;")
    sanitizedUsername = strings.ReplaceAll(sanitizedUsername, "<", "&lt;")
    sanitizedUsername = strings.ReplaceAll(sanitizedUsername, ">", "&gt;")
    
    // Construct the response with proper escaping
    response := fmt.Sprintf("<p>Hello, %s!</p>", sanitizedUsername)
    fmt.Fprint(w, response)
}

func isPrintableASCII(c rune) bool {
    return c >= ' ' && c <= '~'
}