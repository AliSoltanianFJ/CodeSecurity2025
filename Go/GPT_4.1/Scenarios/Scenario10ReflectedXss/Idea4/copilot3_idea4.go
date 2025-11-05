package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

// Security Decision: Only allow usernames with alphanumeric characters, underscores, and hyphens (OWASP recommendation).
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{1,32}$`)

func main() {
    // Security Decision: In production, always use HTTPS (TLS) and set secure headers.
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security Decision: ListenAndServe should use TLS in production.
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* 
helloHandler greets the user by username extracted from the URL.
Security:
- Validates and sanitizes the username.
- Uses context-aware output encoding (html.EscapeString).
- Does not leak internal errors or sensitive info.
- Only allows safe characters in username.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    path := strings.TrimPrefix(r.URL.Path, "/hello/")
    username := strings.TrimSpace(path)

    // Validate username: only allow safe characters, length 1-32
    if !validUsername.MatchString(username) {
        // Security: Do not reveal details about validation failure.
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }

    // Security: Escape username for HTML context to prevent XSS.
    safeUsername := html.EscapeString(username)

    // Security: Set content type explicitly.
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
