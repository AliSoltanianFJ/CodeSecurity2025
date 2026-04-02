package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_]+$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// helloHandler looks for exactly one extra segment after /hello/,
// validates it, escapes it, and then returns a personalized greeting.
//
// Examples:
//   GET /hello/bob       → 200 "<p>Hello, bob!</p>"
//   GET /hello/          → 400 "Username not provided"
//   GET /hello/a/b       → 400 "Invalid username"
//   POST /hello/bob      → 405 "Method not allowed"
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Strip the known prefix and see what's left
    //    r.URL.Path is guaranteed to start with "/hello/" by how we registered the handler
    user := strings.TrimPrefix(r.URL.Path, "/hello/")

    // 3) Ensure they actually provided something,
    //    and that it doesn't contain a second "/" (no path traversal, no extra segments)
    if user == "" {
        http.Error(w, "Username not provided", http.StatusBadRequest)
        return
    }
    if strings.Contains(user, "/") {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 4) Validate against an application-specific pattern
    if !validUsername.MatchString(user) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // 5) Escape to prevent XSS
    safeUser := html.EscapeString(user)

    // 6) Send the response
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUser)
}