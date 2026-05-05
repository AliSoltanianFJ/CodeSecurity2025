// ...existing code...
package main

import (
    "fmt"
    "html/template"
    "log"
    "net/http"
    "regexp"
    "strings"
)

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

// template auto-escapes to prevent reflected XSS (defense-in-depth)
var helloTmpl = template.Must(template.New("hello").Parse(`<p>Hello, {{.}}!</p>`))

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/", indexHandler)
    mux.HandleFunc("/hello/", helloHandler)

    // Wrap handlers with security headers middleware
    handler := securityHeaders(mux)

    // In production, serve over TLS (ListenAndServeTLS) and run with least privilege.
    log.Println("Starting server on :8080")
    if err := http.ListenAndServe(":8080", handler); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

// securityHeaders applies common HTTP hardening headers.
// HSTS is only set when the connection is TLS to avoid customer confusion on plain HTTP.
func securityHeaders(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("Referrer-Policy", "no-referrer")
        // Tight CSP: disallow all remote resources by default, allow inline styles for this simple page.
        w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline';")
        if r.TLS != nil {
            w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        }
        next.ServeHTTP(w, r)
    })
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Extract username from path and canonicalize
    name := strings.TrimPrefix(r.URL.Path, "/hello/")
    name = strings.TrimSpace(name)

    // Input validation: whitelist characters and max length
    if name == "" {
        http.Error(w, "missing username", http.StatusBadRequest)
        return
    }
    if len(name) > 30 {
        http.Error(w, "username too long", http.StatusBadRequest)
        return
    }
    if !usernameRE.MatchString(name) {
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Response: use html/template to ensure safe escaping (prevents reflected XSS)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    if err := helloTmpl.Execute(w, name); err != nil {
        // Avoid exposing internal errors to clients; log minimal info (no PII)
        http.Error(w, "internal error", http.StatusInternalServerError)
        log.Println("template execute error:", err)
        return
    }
}
