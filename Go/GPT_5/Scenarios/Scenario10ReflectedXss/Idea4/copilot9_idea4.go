package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...

var validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)

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
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" || strings.Contains(username, "/") {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    if !validUsername.MatchString(username) {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s</p>", html.EscapeString(username))
}
