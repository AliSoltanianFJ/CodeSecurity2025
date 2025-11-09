package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"
)

// ...existing code...
var usernameRegex = regexp.MustCompile(`^[A-Za-z0-9_-]{1,32}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    // log fatal if server fails to start
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    log.Fatal(srv.ListenAndServe())
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    // Minimal fixed response
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Allow only GET to reduce attack surface (no state-changing actions here)
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic security headers (defense in depth)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    // Disallow framing to prevent clickjacking
    w.Header().Set("X-Frame-Options", "DENY")
    // Prevent MIME sniffing
    w.Header().Set("X-Content-Type-Options", "nosniff")
    // Minimal Referrer Policy
    w.Header().Set("Referrer-Policy", "no-referrer")
    // Content Security Policy: no scripts, no external resources
    w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self';")
    // HSTS (only effective over HTTPS in production)
    w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")

    // Extract username from path: expected form /hello/<username>
    name := strings.TrimPrefix(r.URL.Path, "/hello/")
    // Reject empty or malformed names quickly
    if name == "" {
        http.Error(w, "username required (use /hello/<username>)", http.StatusBadRequest)
        return
    }

    // Validate against strict whitelist: only letters, digits, -, _
    if !usernameRegex.MatchString(name) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Escape before embedding into HTML to prevent reflected XSS
    safe := html.EscapeString(name)

    // Minimal and safe HTML response
    fmt.Fprintf(w, "<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body><p>Hello, %s!</p></body></html>", safe)
}
