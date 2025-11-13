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
        fmt.Fprintf(w, "Error: No username provided")
        return
    }
    
    // Create and send the greeting message
    message := fmt.Sprintf("Hello, %s!", username)
    w.Header().Set("Content-Type", "text/plain")
    w.Write([]byte(message))
}