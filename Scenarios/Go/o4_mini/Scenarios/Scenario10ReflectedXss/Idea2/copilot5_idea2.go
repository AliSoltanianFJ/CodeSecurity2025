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

    // Extract the raw username portion from the path
    raw := strings.TrimPrefix(r.URL.Path, prefix)
    if raw == "" || strings.Contains(raw, "/") {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Decode any percent-encoding (reject if malformed)
    username, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Normalize and enforce length limits to avoid excessive resource usage
    username = strings.TrimSpace(username)
    const maxLen = 50
    if len(username) == 0 || len(username) > maxLen {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Validate allowed characters (prevents XSS, control chars, and unexpected input)
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_.-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape before writing into HTML (defense-in-depth for XSS)
    safe := html.EscapeString(username)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
