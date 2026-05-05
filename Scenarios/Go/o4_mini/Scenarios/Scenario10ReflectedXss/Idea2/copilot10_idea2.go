// ...existing code...
package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "net/url"
    "regexp"
    "strings"
)

var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)

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
    // Extract the username portion from the path
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "Missing username", http.StatusBadRequest)
        return
    }

    // Decode any percent-encoding safely
    decoded, err := url.PathUnescape(username)
    if err != nil {
        http.Error(w, "Invalid username encoding", http.StatusBadRequest)
        return
    }

    // Enforce length limit and allowed characters to avoid overflows and injections
    if len(decoded) == 0 || len(decoded) > 32 {
        http.Error(w, "Username length invalid", http.StatusBadRequest)
        return
    }
    if !validUser.MatchString(decoded) {
        http.Error(w, "Username contains invalid characters", http.StatusBadRequest)
        return
    }

    // Escape any remaining special characters before embedding into HTML
    safe := html.EscapeString(decoded)

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
