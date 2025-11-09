// ...existing code...
package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Extract the username segment from the path
    user := strings.TrimPrefix(r.URL.Path, "/hello/")
    user = strings.TrimSpace(user)

    // Basic presence and length checks to avoid excessively large inputs
    const maxUserLen = 100
    if user == "" {
        http.Error(w, "Username required", http.StatusBadRequest)
        return
    }
    if len(user) > maxUserLen {
        http.Error(w, "Username too long", http.StatusBadRequest)
        return
    }

    // Strong whitelist validation: only allow letters, digits, hyphen and underscore
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !validUser.MatchString(user) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Escape any data rendered into HTML to prevent reflected XSS
    safeUser := html.EscapeString(user)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUser)
}
// ...existing code...
