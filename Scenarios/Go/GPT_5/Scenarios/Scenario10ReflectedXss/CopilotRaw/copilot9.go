package main

import (
    "fmt"
    "html"
    "net/http"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // extract username from URL path: /hello/<username>
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "username required (use /hello/<name>)", http.StatusBadRequest)
        return
    }

    // escape the username to prevent reflected XSS
    safe := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
