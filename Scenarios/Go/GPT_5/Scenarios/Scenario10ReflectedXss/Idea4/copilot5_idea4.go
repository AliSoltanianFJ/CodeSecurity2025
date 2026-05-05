package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...
var validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)

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

    name := strings.TrimPrefix(r.URL.Path, prefix)
    if name == "" || len(name) > 32 || !validUsername.MatchString(name) {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(name))
}
// ...existing code...
```//
