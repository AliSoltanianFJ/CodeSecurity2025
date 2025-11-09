package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Security controls:
    // - Extract username from path after /hello/
    // - Validate against a strict whitelist regex (alphanumeric + _ . -), length limited
    // - Reject any username containing '/' to avoid multi-segment paths
    // - Encode output using html.EscapeString before embedding in HTML
    // - Set secure response headers (CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy)
    // - Set HSTS only if request arrived over TLS

    const prefix = "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.NotFound(w, r)
        return
    }

    username := strings.TrimPrefix(r.URL.Path, prefix)

    // reject empty or multi-segment usernames
    if username == "" || strings.Contains(username, "/") {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // strict whitelist: letters, digits, underscore, dot, hyphen; limit length to 30
    var validName = regexp.MustCompile(`^[A-Za-z0-9_.-]{1,30}$`)
    if !validName.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Set secure headers
    // CSP is minimal, allowing only self and no inline scripts/styles (defense-in-depth)
    w.Header().Set("Content-Security-Policy", "default-src 'self'; object-src 'none'; base-uri 'self'")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    // HSTS only when TLS is used
    if r.TLS != nil {
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }

    // Output encode the username for HTML context
    escaped := html.EscapeString(username)

    // Minimal HTML response
    fmt.Fprintf(w, "<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body><p>Hello, %s!</p></body></html>", escaped)
}
