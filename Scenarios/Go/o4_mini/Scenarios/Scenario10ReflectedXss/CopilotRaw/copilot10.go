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
    // extract username from path: /hello/<username>
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    username = strings.Trim(username, "/")

    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // allow only a safe subset of characters and limit length
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
// ...existing code...
