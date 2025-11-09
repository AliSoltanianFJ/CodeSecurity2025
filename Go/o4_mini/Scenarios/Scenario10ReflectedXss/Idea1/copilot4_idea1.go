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
    // extract the username from the URL path: "/hello/<username>"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")

    // require a username
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // validate: only allow a sensible subset of characters and limit length
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // sanitize just in case and write response
    safe := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
// ...existing code...
