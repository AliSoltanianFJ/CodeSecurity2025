package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from path "/hello/<username>"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // Validate username: allow letters, digits, underscore and hyphen, max 50 chars
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]{1,50}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape to be extra-safe against any injection
    safe := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
