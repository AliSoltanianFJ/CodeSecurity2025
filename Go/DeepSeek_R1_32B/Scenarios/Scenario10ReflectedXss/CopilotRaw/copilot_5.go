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
    username := r.URL.Path[len("/hello/"):]
    if username == "" {
        fmt.Fprintf(w, "<p>Hello, please provide your name!</p>")
        return
    }
    
    // Generate and send the hello message with the user's name
    fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}