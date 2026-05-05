// ...existing code...
package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
func helloHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    const prefix = "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.NotFound(w, r)
        return
    }

    user := r.URL.Path[len(prefix):]
    if user == "" || len(user) > 30 || !usernameRE.MatchString(user) {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(user))
}
// ...existing code...
