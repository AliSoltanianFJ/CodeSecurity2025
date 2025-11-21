package main

import (
    "fmt"
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
    // Extract username from URL path
    path := r.URL.Path
    if !strings.HasPrefix(path, "/hello/") {
        http.Error(w, "Invalid path", http.StatusBadRequest)
        return
    }
    username := strings.TrimPrefix(path, "/hello/")
    
    // Validate input: ensure username is not empty and contains only alphanumeric characters and underscores
    if username == "" || len(username) > 50 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    for _, char := range username {
        if !((char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') || (char >= '0' && char <= '9') || char == '_') {
            http.Error(w, "Invalid username", http.StatusBadRequest)
            return
        }
    }
    
    // Sanitize and prevent XSS by using html/template which auto-escapes
    tmpl := template.Must(template.New("hello").Parse("<p>Hello, {{.}}!</p>"))
    tmpl.Execute(w, username)
}
