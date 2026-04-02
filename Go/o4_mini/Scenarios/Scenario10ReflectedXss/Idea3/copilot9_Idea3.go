package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
    "time"
)

var (
    // precompile the regex once
    validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{1,30}$`)
)

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/", indexHandler)
    mux.HandleFunc("/hello/", helloHandler)

    srv := &http.Server{
        Addr:         ":8080",
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }

    if err := srv.ListenAndServe(); err != nil {
        panic(err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// helloHandler says “Hello, {username}!” on GET /hello/{username}
// It rejects anything other than GET, validates and escapes the username.
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Method check
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Extract the “username” segment from the URL path
    //    we know this handler is registered on "/hello/"
    const prefix = "/hello/"
    path := r.URL.Path
    if !strings.HasPrefix(path, prefix) {
        http.NotFound(w, r)
        return
    }
    username := path[len(prefix):] // could be empty

    // 3) Basic sanity checks
    if username == "" {
        http.NotFound(w, r)
        return
    }
    if !validUsername.MatchString(username) {
        // reject anything with suspicious chars or overly long
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 4) HTML-escape (defense in depth—our regex already restricts)
    safeUsername := html.EscapeString(username)

    // 5) Write the response
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}