// ...existing code...
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
    // Get the part after "/hello/"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    // If there are extra path segments, keep only the first segment
    if i := strings.IndexByte(username, '/'); i >= 0 {
        username = username[:i]
    }

    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // Allow only safe characters and limit length
    var valid = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)
    if !valid.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape for safe HTML output
    safe := html.EscapeString(username)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
// ...existing code...