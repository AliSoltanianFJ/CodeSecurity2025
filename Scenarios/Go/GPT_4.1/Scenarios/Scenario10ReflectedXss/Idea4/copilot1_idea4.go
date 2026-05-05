package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

// Security Note: Always use HTTPS in production to protect data in transit.
// For demonstration, this example uses HTTP. In production, configure TLS.

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security: ListenAndServe should use TLS in production.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
helloHandler greets the user by name, extracted from the URL path.
Security measures:
- Strictly validates username: only allows alphanumeric, underscores, and hyphens (OWASP recommendation).
- Sanitizes output using html.EscapeString to prevent XSS (CWE-79).
- Does not leak internal errors to the client.
- No sensitive data is processed or stored.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path
    // Expected path: /hello/<username>
    path := r.URL.Path
    prefix := "/hello/"
    if !strings.HasPrefix(path, prefix) {
        http.Error(w, "Invalid endpoint", http.StatusNotFound)
        return
    }
    username := strings.TrimPrefix(path, prefix)

    // Validate username: allow only 3-32 chars, alphanumeric, underscore, hyphen
    // Security: Prevents path traversal, injection, and XSS vectors.
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_-]{3,32}$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "Invalid username. Usernames must be 3-32 characters and contain only letters, numbers, underscores, or hyphens.", http.StatusBadRequest)
        return
    }

    // Escape username for HTML output to prevent XSS
    safeUsername := html.EscapeString(username)

    // Security: Use context-aware output encoding (html.EscapeString)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
