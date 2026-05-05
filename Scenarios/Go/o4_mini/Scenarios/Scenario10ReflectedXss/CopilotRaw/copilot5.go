package main

import (
    "fmt"
    "html"
    "net/http"
    "net/url"
    "regexp"
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

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // extract username from URL path: /hello/<username>
    name := strings.TrimPrefix(r.URL.Path, "/hello/")
    if name == "" {
        http.Error(w, "missing username", http.StatusBadRequest)
        return
    }

    // decode percent-encoding (e.g. %20)
    if decoded, err := url.PathUnescape(name); err == nil {
        name = decoded
    }

    // basic validation: length and allowed characters
    if len(name) > 30 {
        http.Error(w, "username too long", http.StatusBadRequest)
        return
    }
    valid := regexp.MustCompile(`^[A-Za-z0-9_.-]+$`)
    if !valid.MatchString(name) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // escape before writing to avoid reflected XSS
    safe := html.EscapeString(name)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
// ...existing code...
