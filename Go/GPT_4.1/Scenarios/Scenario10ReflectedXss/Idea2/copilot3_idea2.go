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
    prefix := "/hello/"
    if !strings.HasPrefix(path, prefix) {
        http.NotFound(w, r)
        return
    }
    username := strings.TrimPrefix(path, prefix)

    // Validate username: allow only letters, numbers, underscores, 3-20 chars
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_]{3,20}$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username. Only letters, numbers, and underscores (3-20 chars) allowed.", http.StatusBadRequest)
        return
    }

    // Escape username to prevent XSS
    safeUsername := html.EscapeString(username)

    // Respond with a safe greeting
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
