package main

import (
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

var (
    // whitelist: letters, digits, underscore, hyphen; length 1..64
    usernameRegex = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)

    // ListenAndServe returns an error if it fails; log it and exit.
    log.Println("Starting server on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
    // Minimal static content; no user input.
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (defense in depth). HSTS should only be set when serving over HTTPS.
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")

    // Extract username from path: /hello/<username>
    username := strings.TrimPrefix(r.URL.Path, "/hello/")
    if username == "" {
        http.Error(w, "missing username", http.StatusBadRequest)
        return
    }

    // Validate against a strict whitelist to prevent XSS and other injection.
    if !usernameRegex.MatchString(username) {
        // Do not echo unvalidated input back to the client.
        http.Error(w, "invalid username", http.StatusBadRequest)
        return
    }

    // Defense-in-depth: encode any output going into HTML context.
    safe := html.EscapeString(username)

    // Render a simple, escaped greeting.
    // The response content-type is already set to text/html.
    _, err := fmt.Fprintf(w, "<p>Hello, %s!</p>", safe)
    if err != nil {
        // Log internal errors without leaking details to client.
        log.Printf("failed to write response: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}
