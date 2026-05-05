package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "missing username", http.StatusBadRequest)
        return
    }

    // Escape to prevent reflected XSS
    safeName := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeName)
}
// ...existing code...
