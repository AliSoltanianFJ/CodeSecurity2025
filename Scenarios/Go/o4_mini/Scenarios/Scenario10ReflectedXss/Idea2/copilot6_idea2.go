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

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
/* ...existing code... */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    const maxLen = 30
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    // Extract the part after "/hello/"
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    if raw == "" {
        http.Error(w, "Missing username", http.StatusBadRequest)
        return
    }

    // Decode percent-encoding safely
    name, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "Invalid username encoding", http.StatusBadRequest)
        return
    }

    // Only consider the first path segment (prevent trailing slashes or extra path parts)
    if i := strings.Index(name, "/"); i != -1 {
        name = name[:i]
    }

    // Basic length check to avoid excessive memory use
    if len(name) == 0 || len(name) > maxLen {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Allow only a safe subset of characters
    var valid = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !valid.MatchString(name) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Escape before embedding in HTML to prevent XSS
    safe := html.EscapeString(name)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
