package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
)

// Security Note: Always use HTTPS in production to protect data in transit.
// For demonstration, this example uses HTTP. In production, configure TLS.

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // Security: ListenAndServeTLS should be used in production for HTTPS.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/*
helloHandler greets the user by name, extracted from the URL path.
Security measures:
- Input is strictly validated (only allows alphanumeric, underscore, hyphen, 3-32 chars).
- Input is contextually HTML-escaped before output to prevent XSS (CWE-79).
- No sensitive information is leaked in errors.
- No secrets or credentials are hardcoded.
- Principle of least privilege: no file or DB access.
- All user input is sanitized and validated.
*/
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from URL path: /hello/<username>
    username := r.URL.Path[len("/hello/"):]
    // Validate username: only allow 3-32 alphanumeric, underscore, hyphen
    validUsername := regexp.MustCompile(`^[a-zA-Z0-9_-]{3,32}$`)
    if !validUsername.MatchString(username) {
        // Security: Do not reveal details about validation failure
        http.Error(w, "Invalid username.", http.StatusBadRequest)
        return
    }
    // Security: HTML-escape username to prevent XSS
    safeUsername := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
