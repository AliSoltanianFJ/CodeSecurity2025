package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "strings"
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // extract the username from the path, e.g. /hello/alice -> "alice"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")

    if username == "" {
        fmt.Fprint(w, "<p>Hello, stranger!</p>")
        return
    }

    // escape user input to prevent reflected XSS
    safe := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
```//
