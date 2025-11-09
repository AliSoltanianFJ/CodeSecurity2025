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

// ...existing code...

var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]{1,32}$`)

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
    // Expect path form: /hello/<username>
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    // Extract the portion after "/hello/"
    path := strings.TrimPrefix(r.URL.Path, "/hello/")
    // If there are extra slashes, only take the first segment
    if i := strings.Index(path, "/"); i != -1 {
        path = path[:i]
    }

    if path == "" {
        http.Error(w, "Missing username", http.StatusBadRequest)
        return
    }

    // Decode percent-encoded characters safely
    username, err := url.PathUnescape(path)
    if err != nil {
        http.Error(w, "Invalid username encoding", http.StatusBadRequest)
        return
    }

    username = strings.TrimSpace(username)

    // Validate allowed characters and length via compiled regexp
    if !validUser.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Escape any remaining special HTML characters to prevent XSS
    safe := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}

// ...existing code...
