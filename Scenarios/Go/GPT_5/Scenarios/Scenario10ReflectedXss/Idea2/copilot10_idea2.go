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
    path := r.URL.Path
    if len(path) <= 7 || path[:7] != "/hello/" {
        http.NotFound(w, r)
        return
    }
    username := path[7:]
    // Validate and sanitize input: trim whitespace, limit length to prevent potential issues, and escape HTML to prevent XSS
    username = strings.TrimSpace(username)
    if len(username) == 0 || len(username) > 100 {
        username = "World"
    }
    // Escape HTML to prevent cross-site scripting
    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
