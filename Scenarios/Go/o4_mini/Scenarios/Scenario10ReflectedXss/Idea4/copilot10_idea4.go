package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...

var usernameRe = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

// ...existing code...

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce allowed method
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    // Extract username from path: /hello/<username>
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    // Remove any trailing slash if user requested /hello/<user>/
    username = strings.TrimSuffix(username, "/")

    // Require non-empty username
    if username == "" {
        http.Error(w, "Username required", http.StatusBadRequest)
        return
    }

    // Whitelist validation: only allow letters, digits, underscore, hyphen; length 1-30.
    if !usernameRe.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Defense-in-depth: HTML-escape even after whitelist validation
    escaped := html.EscapeString(username)

    // Minimal, safe reflected output
    fmt.Fprintf(w, "<p>Hello, %s!</p>", escaped)
}

// setSecurityHeaders applies response headers to harden against common web attacks.
// Explanations:
// - Content-Security-Policy: blocks inline scripts/styles from untrusted sources; here we
//   disallow scripts and allow only same-origin resources if needed.
// - X-Content-Type-Options: prevents MIME sniffing.
// - X-Frame-Options: mitigates clickjacking.
// - Referrer-Policy: avoids leaking referrer to third parties.
// - Cache-Control: avoid caching user-specific responses.
// - Strict-Transport-Security (HSTS) is set only when request arrived over TLS.
func setSecurityHeaders(w http.ResponseWriter, r *http.Request) {
    // CSP: no scripts, only same-origin for resources if used
    w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none'")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate")
    // Set HSTS only when connection is TLS
    if r.TLS != nil {
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }
}
