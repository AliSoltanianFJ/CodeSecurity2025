package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "net/url"
    "strings"
)

func main() {
    // serve the index page at "/"
    http.HandleFunc("/", indexHandler)

    // serve the hello page at "/hello/<username>"
    http.HandleFunc("/hello/", helloHandler)

    log.Printf("listening on :8080…")
    // always check ListenAndServe errors in real code
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    // we only expect GET here, but it's so harmless we won't even check.
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// helloHandler says "Hello, <username>!" for GET /hello/<username>
// and returns 4xx on anything unexpected.
func helloHandler(w http.ResponseWriter, r *http.Request) {
    const prefix = "/hello/"

    // 1) Method check
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Make sure the path actually has the prefix, then strip it.
    //    (In practice http.ServeMux already only routes /hello/ here,
    //     but it doesn’t trim the URL for you.)
    path := r.URL.Path
    if !strings.HasPrefix(path, prefix) {
        http.NotFound(w, r)
        return
    }
    rawUsername := path[len(prefix):]

    // 3) Reject empty usernames
    if rawUsername == "" {
        http.Error(w, "username not provided", http.StatusBadRequest)
        return
    }

    // 4) Reject any extra slashes: e.g. /hello/alice/ or /hello/alice/bob
    //    so we only ever accept exactly one segment after /hello/
    if strings.Contains(rawUsername, "/") {
        http.NotFound(w, r)
        return
    }

    // 5) URL-decode in case the client sent percent-escapes
    username, err := url.PathUnescape(rawUsername)
    if err != nil {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // 6) HTML-escape to avoid XSS if the user name contains e.g. "<script>"
    safeName := html.EscapeString(username)

    // 7) Send the response
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeName)
}