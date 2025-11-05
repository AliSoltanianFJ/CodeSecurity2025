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
    // Extract the username from the URL path
    path := r.URL.Path
    // "/hello/" is 7 characters
    if len(path) <= len("/hello/") {
        http.Error(w, "Username not provided", http.StatusBadRequest)
        return
    }
    username := path[len("/hello/"):]
    // Sanitize username to prevent XSS
    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
