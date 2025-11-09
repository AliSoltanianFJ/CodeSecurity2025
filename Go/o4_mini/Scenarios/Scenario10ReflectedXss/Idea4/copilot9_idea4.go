package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "net/url"
    "regexp"
    "strings"
)

var validUsername = regexp.MustCompile(`^[A-Za-z0-9_-]{1,30}$`)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Response hardening headers (defense-in-depth)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // Restrictive CSP: no external resources allowed; only inline safe content if needed
    w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")

    // Extract username from path: expected /hello/<username>
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    if raw == "" {
        http.Error(w, "missing username", http.StatusBadRequest)
        return
    }

    // Canonicalize percent-encoding. Reject invalid encodings.
    username, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "invalid username encoding", http.StatusBadRequest)
        return
    }

    // Whitelist validation: only allow a limited charset and length to prevent injection and overflow.
    if !validUsername.MatchString(username) {
        // Echo a safe version of the problematic input (escaped) to avoid leaking raw content
        safe := html.EscapeString(username)
        http.Error(w, "invalid username: "+safe, http.StatusBadRequest)
        return
    }

    // Output encoding: escape any residual special chars before writing into HTML context.
    safe := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
