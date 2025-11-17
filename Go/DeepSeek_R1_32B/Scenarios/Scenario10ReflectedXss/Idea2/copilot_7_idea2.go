package main

import (
    "fmt"
    "html"
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
    pathSegments := strings.Split(r.URL.Path, "/")
    if len(pathSegments) < 3 {
        http.Error(w, "Invalid URL path", http.StatusBadRequest)
        return
    }
    username := pathSegments[2]

    // Validate username input to prevent security issues
    maxUsernameLength := 30
    if len(username) > maxUsernameLength || !isSafeUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// isSafeUsername checks if the username contains only allowed characters and doesn't contain any potential XSS vectors
func isSafeUsername(s string) bool {
    for _, c := range s {
        if (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' {
            continue
        }
        return false
    }
    return true
}