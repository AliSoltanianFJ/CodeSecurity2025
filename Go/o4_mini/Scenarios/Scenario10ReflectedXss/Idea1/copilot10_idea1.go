// ...existing code...
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
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Expect URL of form "/hello/<username>" — take only the first path segment after "/hello/"
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    if i := strings.IndexByte(raw, '/'); i >= 0 {
        raw = raw[:i]
    }

    if raw == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // Decode percent-encoding (e.g. %20)
    username, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "invalid username encoding", http.StatusBadRequest)
        return
    }

    // Basic validation: allow letters, numbers, underscore and hyphen; limit length
    const maxLen = 32
    if len(username) == 0 || len(username) > maxLen {
        http.Error(w, "invalid username length", http.StatusBadRequest)
        return
    }
    valid := regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !valid.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape before writing to HTML to prevent XSS
    safe := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
