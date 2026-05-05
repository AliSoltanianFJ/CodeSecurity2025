package main

import (
    "fmt"
    "net/http"
    "strings"
    "html"
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
    // "/hello/<username>"
    parts := strings.SplitN(path, "/", 3)
    if len(parts) < 3 || parts[2] == "" {
        http.Error(w, "Username not provided", http.StatusBadRequest)
        return
    }
    username := html.EscapeString(parts[2]) // Escape to prevent XSS
    fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}
