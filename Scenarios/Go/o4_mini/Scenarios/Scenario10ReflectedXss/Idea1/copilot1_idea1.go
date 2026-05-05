package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "net/url"
    "regexp"
    "strings"
)

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
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    // Extract first path segment after "/hello/"
    seg := strings.TrimPrefix(r.URL.Path, "/hello/")
    if seg == "" {
        http.Error(w, "missing username", http.StatusBadRequest)
        return
    }
    seg = strings.SplitN(seg, "/", 2)[0]

    // Decode percent-encoding (e.g. /hello/john%20doe)
    username, err := url.PathUnescape(seg)
    if err != nil {
        http.Error(w, "invalid username encoding", http.StatusBadRequest)
        return
    }

    // Validate: allow only a safe subset of characters and reasonable length
    valid := regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
    if !valid.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Extra safety: escape before writing into HTML
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
