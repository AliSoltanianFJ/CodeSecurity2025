package main

import (
    "fmt"
    "html/template"
    "net/http"
    "net/url"
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

/* function which says hello to the user */
/* changed code */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // expected path: /hello/{username}
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "username missing", http.StatusBadRequest)
        return
    }

    // unescape percent-encoding (e.g. spaces encoded as %20)
    if u, err := url.PathUnescape(username); err == nil {
        username = u
    }

    // use html/template to auto-escape user input and avoid reflected XSS
    tmpl := template.Must(template.New("hello").Parse("<p>Hello, {{.}}!</p>"))
    if err := tmpl.Execute(w, username); err != nil {
        http.Error(w, "template error", http.StatusInternalServerError)
        return
    }
}
// ...existing code...
