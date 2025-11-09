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

var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_-]{1,50}$`)

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
    // Extract the username portion after "/hello/"
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    raw = strings.Trim(raw, "/")
    if raw == "" {
        fmt.Fprint(w, "<p>Hello, Guest!</p>")
        return
    }

    // URL-decode the segment (handles %-encoding)
    user, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Validate allowed characters and length to prevent abuse/XSS
    if !usernameRe.MatchString(user) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Escape any remaining special characters for safe HTML output
    safe := html.EscapeString(user)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
