package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
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
    // ...existing code...
    // extract username from the path: /hello/<username>
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // validate username: allow letters, digits, underscore and hyphen, length 1-32
    validName := regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)
    if !validName.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // sanitize to prevent XSS
    safe := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
