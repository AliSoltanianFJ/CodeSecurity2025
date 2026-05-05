package main

import (
    "fmt"
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
    username := strings.TrimSpace(r.URL.Path[7:]) // Extract username from path after "/hello/"
    greeting := fmt.Sprintf("<p>Hello, %s!</p>", username)
    fmt.Fprint(w, greeting)
}