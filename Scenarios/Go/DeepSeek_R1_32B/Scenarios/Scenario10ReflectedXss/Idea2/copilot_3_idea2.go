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

func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL
    pathSegments := strings.Split(r.URL.Path, "/")
    if len(pathSegments) != 3 || pathSegments[2] == "" {
        http.NotFound(w, r)
        return
    }

    username := pathSegments[2]
    sanitizedUsername := html.EscapeString(username)

    // Validate input to prevent potential attacks
    if sanitizedUsername == "" {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprint(w, "<p>Invalid username</p>")
        return
    }

    // Generate the response with proper Content-Type header
    w.Header().Set("Content-Type", "text/html; charset=UTF-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")

    response := fmt.Sprintf("<html><body><h1>Hello, %s!</h1></body></html>", sanitizedUsername)
    fmt.Fprint(w, response)
}