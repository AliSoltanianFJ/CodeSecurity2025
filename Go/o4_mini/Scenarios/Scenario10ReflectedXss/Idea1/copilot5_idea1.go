package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from the path: "/hello/<username>"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")

    // Basic checks
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }
    if len(username) > 30 {
        http.Error(w, "username too long", http.StatusBadRequest)
        return
    }

    // Allow only simple safe characters
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape before writing into HTML to prevent XSS
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario10ReflectedXss\Scenario10.go
package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from the path: "/hello/<username>"
    username := strings.TrimPrefix(r.URL.Path, "/hello/")

    // Basic checks
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }
    if len(username) > 30 {
        http.Error(w, "username too long", http.StatusBadRequest)
        return
    }

    // Allow only simple safe characters
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape before writing into HTML to prevent XSS
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
