package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

// Security Decision: Only allow alphanumeric usernames (plus underscore and hyphen), 3-32 chars.
// This prevents XSS, path traversal, and other injection attacks.
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{3,32}$`)

func main() {
    // Security: Always use HTTPS in production. This example uses HTTP for local development only.
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security: ListenAndServe returns an error; handle it securely.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Do not leak sensitive info in logs.
        fmt.Println("Server failed to start")
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* 
helloHandler greets the user by username from the URL path.
Security:
- Strictly validates and sanitizes the username.
- Uses context-aware output encoding (html.EscapeString).
- Does not leak sensitive info in error messages.
- Does not trust user input for HTML output.
- Does not use deprecated or unsafe APIs.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from path: /hello/<username>
    path := r.URL.Path
    parts := strings.Split(path, "/")
    if len(parts) < 3 || parts[2] == "" {
        http.Error(w, "Username required", http.StatusBadRequest)
        return
    }
    username := parts[2]

    // Validate username strictly
    if !validUsername.MatchString(username) {
        // Security: Do not echo back invalid input.
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Security: Escape username for HTML context to prevent XSS.
    safeUsername := html.EscapeString(username)

    // Security: Use a safe format for output.
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
