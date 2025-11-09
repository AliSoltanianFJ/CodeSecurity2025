package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

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
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // validate username to a safe pattern (alphanum, underscore, hyphen; max 30 chars)
    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // sanitize before reflecting into HTML to prevent XSS
    safe := html.EscapeString(username)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario10ReflectedXss\Scenario10.go
package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

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
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // validate username to a safe pattern (alphanum, underscore, hyphen; max 30 chars)
    if !usernameRe.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // sanitize before reflecting into HTML to prevent XSS
    safe := html.EscapeString(username)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
