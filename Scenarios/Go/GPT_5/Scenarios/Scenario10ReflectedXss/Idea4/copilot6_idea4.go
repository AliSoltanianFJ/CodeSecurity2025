// ...existing code...
package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
func helloHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const prefix = "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    name := r.URL.Path[len(prefix):]
    if name == "" {
        http.Error(w, "Missing username", http.StatusBadRequest)
        return
    }

    if !usernameRE.MatchString(name) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    safe := html.EscapeString(name)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}