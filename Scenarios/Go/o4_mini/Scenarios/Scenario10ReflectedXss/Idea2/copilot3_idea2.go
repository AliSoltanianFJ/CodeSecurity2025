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
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
// ...existing code...
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Extract username from path: /hello/<username>
    username := strings.TrimPrefix(r.URL.Path, "/hello/")

    // Basic checks
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }
    if strings.Contains(username, "/") {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Validate allowed characters and enforce a reasonable length limit (defense-in-depth).
    // Only allow letters, digits, underscore and hyphen, up to 32 chars.
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape before reflecting into HTML (extra safety even though validation restricts chars)
    safe := html.EscapeString(username)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
