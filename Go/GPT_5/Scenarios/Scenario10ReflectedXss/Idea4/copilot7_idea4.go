package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

var usernameRegex = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const prefix = "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.NotFound(w, r)
        return
    }

    username := r.URL.Path[len(prefix):]
    if username == "" {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
```//
