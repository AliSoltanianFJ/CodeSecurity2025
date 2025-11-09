// ...existing code...
package main

import (
    "fmt"
    "html"
    "net/http"
    "net/url"
    "strings"
    "unicode/utf8"
)

// ...existing code...
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)
    http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    const maxRunes = 64

    // Only allow GET for this simple endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Extract the username part from the path: /hello/<username>
    raw := strings.TrimPrefix(r.URL.Path, "/hello/")
    if raw == "" || raw == r.URL.Path {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // Decode any percent-encoding safely
    username, err := url.PathUnescape(raw)
    if err != nil {
        http.Error(w, "invalid username encoding", http.StatusBadRequest)
        return
    }

    username = strings.TrimSpace(username)
    if username == "" {
        http.Error(w, "username required", http.StatusBadRequest)
        return
    }

    // Enforce maximum length in runes to prevent any resource/buffer issues
    if utf8.RuneCountInString(username) > maxRunes {
        http.Error(w, "username too long", http.StatusBadRequest)
        return
    }

    // Validate allowed characters: ASCII letters, digits, underscore, hyphen
    for _, r := range username {
        if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9') || r == '_' || r == '-' {
            continue
        }
        http.Error(w, "invalid characters in username", http.StatusBadRequest)
        return
    }

    // Security headers and safe HTML output (escape to prevent XSS)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    safe := html.EscapeString(username)
    fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
}
// ...existing code...
