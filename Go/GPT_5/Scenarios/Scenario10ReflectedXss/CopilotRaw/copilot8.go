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
    // Extract username from URL path (after "/hello/")
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "Missing username in URL (use /hello/<username>)", http.StatusBadRequest)
        return
    }

    // Escape to prevent reflected XSS
    usernameEscaped := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", usernameEscaped)
}
// ...existing code...
