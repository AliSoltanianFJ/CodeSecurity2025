package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
)

// Security Decision: Only allow usernames with 3-20 alphanumeric characters (plus underscore and hyphen).
// This prevents XSS, path traversal, and other injection attacks.
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{3,20}$`)

func main() {
    // Security Decision: Always use HTTPS in production. For demo, this is HTTP.
    // In production, use http.ListenAndServeTLS with valid certificates.
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security Decision: Do not use default nil handler in production; consider custom error handlers.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* 
helloHandler greets the user by username from the URL path.
- Validates and sanitizes the username to prevent XSS and injection.
- Uses context-aware output encoding (html.EscapeString).
- Does not leak internal errors to the client.
- Only allows safe usernames (alphanumeric, underscore, hyphen, 3-20 chars).
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    username := r.URL.Path[len("/hello/"):]
    if !validUsername.MatchString(username) {
        // Security: Do not reveal details about validation failure.
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }

    // Security: Escape username before outputting to HTML to prevent XSS.
    safeUsername := html.EscapeString(username)

    // Security: Use a simple, static template to avoid template injection.
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
