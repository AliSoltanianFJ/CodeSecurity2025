package main

import (
    "fmt"
    "net/http"
    "html"
    "regexp"
)

var userRE = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

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
    // extract username from path "/hello/<username>"
    username := ""
    if len(r.URL.Path) > len("/hello/") {
        username = r.URL.Path[len("/hello/"):]
    }

    // require a username
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // validate allowed characters and length to avoid injection or abuse
    if !userRE.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // safe to output after validation; still escape to be extra-safe
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
