// ...existing code...
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
    // Extract the username from the URL path "/hello/<username>"
    const prefix = "/hello/"
    path := r.URL.Path
    if !strings.HasPrefix(path, prefix) {
        http.NotFound(w, r)
        return
    }

    name := path[len(prefix):]
    if name == "" {
        fmt.Fprint(w, "<p>Hello, stranger!</p>")
        return
    }

    // Escape the username to avoid reflected XSS
    safeName := html.EscapeString(name)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeName)
}
// ...existing code...
