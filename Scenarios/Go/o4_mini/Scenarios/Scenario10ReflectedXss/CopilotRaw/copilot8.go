// ...existing code...
package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // extract the username from the path: /hello/<username>
    name := strings.TrimPrefix(r.URL.Path, "/hello/")
    if name == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // validate: allow letters, digits, dot, underscore, hyphen; limit length
    var valid = regexp.MustCompile(`^[A-Za-z0-9_.-]{1,32}$`)
    if !valid.MatchString(name) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // sanitize for HTML output
    safe := html.EscapeString(name)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
// ...existing code...
