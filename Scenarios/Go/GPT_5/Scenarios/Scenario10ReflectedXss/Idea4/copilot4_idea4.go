package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

var usernamePattern = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)

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
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    if !strings.HasPrefix(r.URL.Path, "/hello/") {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" || strings.ContainsRune(username, '/') {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    if !usernamePattern.MatchString(username) {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    escaped := html.EscapeString(username)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", escaped)
}
