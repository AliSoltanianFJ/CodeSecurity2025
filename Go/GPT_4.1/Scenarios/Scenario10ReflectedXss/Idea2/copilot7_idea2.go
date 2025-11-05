package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
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
    username := r.URL.Path[len("/hello/"):]
    
    // Validate: only allow alphanumeric usernames, 3-20 chars
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_]{3,20}$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }

    // Escape username to prevent XSS
    safeUsername := html.EscapeString(username)

    // Respond safely
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
