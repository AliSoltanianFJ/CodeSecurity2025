package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

var validName = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" || strings.Contains(username, "/") || !validName.MatchString(username) {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
