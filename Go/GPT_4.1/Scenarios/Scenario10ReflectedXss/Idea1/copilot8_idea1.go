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
    // Extract the username from the URL path
    // The path will be in the form "/hello/<username>"
    path := r.URL.Path
    parts := strings.SplitN(path, "/", 3)
    username := ""
    if len(parts) >= 3 {
        username = parts[2]
    }
    // Escape username to prevent XSS
    escapedUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", escapedUsername)
}
