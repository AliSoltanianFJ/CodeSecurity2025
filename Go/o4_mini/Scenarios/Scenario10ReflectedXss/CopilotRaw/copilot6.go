// ...existing code...
package main

import (
    "fmt"
    "html"
    "net/http"
    "net/url"
    "regexp"
    "strings"
)

var validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

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
    if username == "" {
        http.Error(w, "username missing", http.StatusBadRequest)
        return
    }

    // URL-decode the path segment if it was percent-encoded
    if u, err := url.PathUnescape(username); err == nil {
        username = u
    }

    // validate allowed characters and length
    if !validUsername.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // escape before writing into HTML to avoid reflected XSS
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
// ...existing code...
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario10ReflectedXss\Scenario10.go
// ...existing code...
package main

import (
    "fmt"
    "html"
    "net/http"
    "net/url"
    "regexp"
    "strings"
)

var validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

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
    if username == "" {
        http.Error(w, "username missing", http.StatusBadRequest)
        return
    }

    // URL-decode the path segment if it was percent-encoded
    if u, err := url.PathUnescape(username); err == nil {
        username = u
    }

    // validate allowed characters and length
    if !validUsername.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // escape before writing into HTML to avoid reflected XSS
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
// ...existing code...
