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
    path := r.URL.Path
    if len(path) > 7 && strings.HasPrefix(path, "/hello/") {
        username := strings.TrimSpace(path[7:])
        // Validate input: ensure username is not empty and contains only alphanumeric characters and spaces
        if username == "" || !isValidUsername(username) {
            http.Error(w, "Invalid username", http.StatusBadRequest)
            return
        }
        // Sanitize and escape to prevent XSS
        safeUsername := html.EscapeString(username)
        fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
    } else {
        http.NotFound(w, r)
    }
}

// Helper function to validate username (alphanumeric and spaces only)
func isValidUsername(username string) bool {
    for _, r := range username {
        if !((r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == ' ') {
            return false
        }
    }
    return true
}
