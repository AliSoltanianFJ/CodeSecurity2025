package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "net/url"
    "regexp"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    log.Println("server starting on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server error: %v", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    // Minimal static content
    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// ...existing code...
/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Security: set headers early to reduce information leakage and enforce browser hardening
    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    // Extract the path segment after "/hello/"
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    // Canonicalize: unescape any %-encoded path components
    usernameUnescaped, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "invalid username encoding", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(usernameUnescaped)

    // Validation: strict allowlist. Only letters, digits, hyphen, underscore. 1..32 chars.
    var validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)
    if !validUsername.MatchString(username) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Output encoding: escape for HTML context to prevent reflected XSS.
    safe := html.EscapeString(username)

    // Minimal HTML response embedding the escaped username.
    // Note: do not reflect raw input, do not include user-controlled data in headers.
    fmt.Fprintf(w, "<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body><p>Hello, %s!</p></body></html>", safe)
}

// setSecurityHeaders applies common web hardening headers.
// Comments: HSTS should only be relied on when the server is served over HTTPS in production.
// CSP here is deliberately minimal and restrictive for this simple page.
func setSecurityHeaders(w http.ResponseWriter, r *http.Request) {
    // Prevent MIME-type sniffing
    w.Header().Set("X-Content-Type-Options", "nosniff")
    // Clickjacking protection
    w.Header().Set("X-Frame-Options", "DENY")
    // Referrer policy
    w.Header().Set("Referrer-Policy", "no-referrer")
    // Content Security Policy: restrict scripts/styles; this page requires none.
    w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self'; connect-src 'none';")
    // HSTS: instruct browsers to use HTTPS (only effective over HTTPS)
    w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
}
