package main

import (
    "fmt"
    "net/http"
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
    // Extract the username from the URL path
    pathParts := r.URL.Path.Split('/')
    username := "Anonymous"
    if len(pathParts) > 2 && pathParts[2] != "" {
        username = pathParts[2]
    }
    fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}