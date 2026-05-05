package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
)

// Security: Only allow usernames with safe characters (alphanumeric, underscore, hyphen, 3-32 chars)
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{3,32}$`)

func main() {
    // Security: Always use HTTPS in production. For demo, this is HTTP.
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security: Use a non-privileged port and proper error handling.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("Server failed to start: %v", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
helloHandler greets the user by username from the URL path.
Security:
- Strictly validates and sanitizes the username.
- Uses context-aware HTML escaping to prevent XSS (CWE-79).
- Does not leak internal errors to the client.
- Does not use user input in any sensitive operation.
- No secrets or credentials are handled here.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    username := r.URL.Path[len("/hello/"):]
    // Validate username strictly
    if !validUsername.MatchString(username) {
        // Security: Do not reveal details about validation failure
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }
    // Security: Escape username for HTML context to prevent XSS
    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
