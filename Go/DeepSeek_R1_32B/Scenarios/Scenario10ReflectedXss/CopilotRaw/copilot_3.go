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

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract the username from the URL path
    username := "Guest"
    pathParts := r.URL.Path.Split("/")
    if len(pathParts) > 2 {
        username = pathParts[2]
    }
    
    // Send the greeting response
    fmt.Fprintf(w, "Hello, %s!\n", username)
}