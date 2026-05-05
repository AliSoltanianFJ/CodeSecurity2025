// ...existing code...
package main

import (
    "fmt"
    "html"
    "net/http"
    "net/url"
    "regexp"
    "strings"
)

var validUsername = regexp.MustCompile(`^[A-Za-z0-9_\-]{1,30}$`)

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
    const prefix = "/hello/"

    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.NotFound(w, r)
        return
    }

    raw := strings.TrimPrefix(r.URL.Path, prefix)
    if raw == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // decode percent-encoded path segment (e.g. /hello/John%20Doe)
    name, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // validate allowed characters and length
    if !validUsername.MatchString(name) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // escape before output to avoid reflected XSS
    safe := html.EscapeString(name)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
// ...existing code...
