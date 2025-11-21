package main

import (
    "html/template"
    "net/http"
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
    // Extract username from URL path after "/hello/"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        username = "guest"
    }

    // Use html/template for secure rendering to prevent XSS
    tmpl := template.Must(template.New("hello").Parse("<p>Hello, {{.}}!</p>"))
    tmpl.Execute(w, username)
}
