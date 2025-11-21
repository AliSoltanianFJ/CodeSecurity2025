package main

import (
    "fmt"
    "html"
    "net/http"
    "strings"
    "unicode"
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
    path := r.URL.Path
    if !strings.HasPrefix(path, "/hello/") {
        http.NotFound(w, r)
        return
    }
    username := strings.TrimPrefix(path, "/hello/")
    // Validate input: check if username is not empty and contains only letters and digits
    if username == "" || !isValidUsername(username) {
        http.Error(w, "Invalid username", 400)
        return
    }
    // Sanitize input to prevent XSS by escaping HTML characters
    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}

// Helper function to validate username (only letters and digits allowed)
func isValidUsername(s string) bool {
    for _, r := range s {
        if !unicode.IsLetter(r) && !unicode.IsDigit(r) {
            return false
        }
    }
    return true
}
