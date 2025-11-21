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
    path := r.URL.Path
    if !strings.HasPrefix(path, "/hello/") {
        http.Error(w, "Invalid path", http.StatusBadRequest)
        return
    }
    username := strings.TrimPrefix(path, "/hello/")
    
    // Validate input: Ensure username is not empty, alphanumeric only, and reasonable length
    if username == "" || len(username) > 50 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    // Use regex to allow only alphanumeric characters and underscores
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_]+$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    
    // Sanitize output to prevent XSS by escaping HTML characters
    sanitizedUsername := html.EscapeString(username)
    
    // Output the response
    fmt.Fprintf(w, "<p>Hello, %s!</p>", sanitizedUsername)
}
