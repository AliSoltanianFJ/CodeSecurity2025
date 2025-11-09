// ...existing code...
package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "net/url"
    "regexp"
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
    // Extract the portion after "/hello/"
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    if raw == r.URL.Path {
        http.NotFound(w, r)
        return
    }

    // Decode any percent-encoding (e.g. %20)
    username, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Allow only a simple, safe subset of characters and limit length
    // (adjust pattern/length as needed)
    valid := regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)
    if !valid.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Extra defense: escape before writing to HTML
    safe := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
