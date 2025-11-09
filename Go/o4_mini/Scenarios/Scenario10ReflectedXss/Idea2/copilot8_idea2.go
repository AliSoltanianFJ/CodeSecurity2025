// ...existing code...
package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
    "unicode/utf8"
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
func helloHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    // Extract username from path: "/hello/<username>"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // Limit length by rune count to avoid excessive memory use
    if utf8.RuneCountInString(username) > 64 {
        http.Error(w, "username too long", http.StatusBadRequest)
        return
    }

    // Whitelist allowed characters: letters, digits, dot, underscore, hyphen
    var validUser = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape output to prevent XSS
    safe := html.EscapeString(username)

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
