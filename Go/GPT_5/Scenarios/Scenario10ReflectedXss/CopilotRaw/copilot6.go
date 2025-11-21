// ...existing code...
package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "strings"
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path after "/hello/"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        username = "guest"
    }

    // Escape user-provided content to prevent reflected XSS
    safeName := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeName)
}
// ...existing code...
