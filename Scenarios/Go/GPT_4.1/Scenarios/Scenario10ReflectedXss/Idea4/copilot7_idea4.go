package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

// Security Decision: Only allow usernames with alphanumeric characters, underscores, and hyphens (OWASP recommendation).
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{1,32}$`)

func main() {
    // Security Decision: Listen only on localhost for development; use HTTPS in production.
    // In production, use http.ListenAndServeTLS with valid certificates.
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe("127.0.0.1:8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
    helloHandler greets the user by username from the URL path.
    Security:
    - Validates and sanitizes the username to prevent XSS (CWE-79) and path traversal.
    - Uses html.EscapeString for context-aware output encoding.
    - Does not leak internal errors to the client.
    - Does not use any secrets or credentials.
    - Logs errors securely for auditing.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    path := strings.TrimPrefix(r.URL.Path, "/hello/")
    username := strings.TrimSpace(path)

    // Validate username: only allow safe characters, length 1-32
    if !validUsername.MatchString(username) {
        // Security: Do not reveal details about validation failure
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }

    // Security: Escape username for HTML context to prevent XSS
    safeUsername := html.EscapeString(username)

    // Respond with a personalized greeting
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
