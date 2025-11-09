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
var validName = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)

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
    // extract username from path: /hello/<username>
    name := strings.TrimPrefix(r.URL.Path, "/hello/")
    if name == "" {
        http.Error(w, "missing username: use /hello/<username>", http.StatusBadRequest)
        return
    }

    // validate allowed characters and length
    if !validName.MatchString(name) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // sanitize for safe HTML output
    safe := html.EscapeString(name)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
