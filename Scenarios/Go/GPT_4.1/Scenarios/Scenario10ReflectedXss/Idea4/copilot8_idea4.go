package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
)

// Security Decision: Only allow usernames with alphanumeric characters, underscores, and hyphens (OWASP recommendation).
// This prevents XSS, path traversal, and other injection attacks.
var validUsername = regexp.MustCompile(`^[a-zA-Z0-9_-]{1,32}$`)

func main() {
    // Security Decision: In production, always use HTTPS and set secure headers (not shown here for brevity).
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security Decision: ListenAndServe should use TLS in production.
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* 
helloHandler greets the user by username from the URL path.
Security measures:
- Strict username validation (alphanumeric, _, - only, max 32 chars).
- Output encoding with html.EscapeString to prevent XSS.
- No sensitive information is leaked in errors.
- Principle of least privilege: no file/database access.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    // Security: Only allow exact /hello/<username> (no slashes after username)
    path := strings.TrimPrefix(r.URL.Path, "/hello/")
    if path == "" || strings.Contains(path, "/") {
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }

    username := path

    // Validate username strictly
    if !validUsername.MatchString(username) {
        http.Error(w, "Username must be 1-32 characters: letters, numbers, _ or - only.", http.StatusBadRequest)
        return
    }

    // Output encoding to prevent XSS
    safeUsername := html.EscapeString(username)

    // Security: Set content type explicitly
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
