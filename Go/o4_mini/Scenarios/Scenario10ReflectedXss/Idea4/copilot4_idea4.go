package main

import (
    "html/template"
    "log"
    "net"
    "net/http"
    "net/url"
    "regexp"
    "strings"
    "time"
)

var (
    // strict whitelist: letters, digits, dot, underscore, hyphen; max 64 chars
    usernameRE = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)
    helloTmpl  = template.Must(template.New("hello").Parse(`<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Hello</title>
</head>
<body>
<p>Hello, {{.}}!</p>
</body>
</html>`))
)

// indexHandler serves a small safe page.
func indexHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w)
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    _, _ = w.Write([]byte("<p>Hello, World!</p>"))
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this read-only endpoint
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    setSecurityHeaders(w)

    // Expect path /hello/{username}
    // Split path and take the segment after /hello/
    // Use PathUnescape to canonicalize %XX sequences
    prefix := "/hello/"
    if !strings.HasPrefix(r.URL.Path, prefix) {
        http.NotFound(w, r)
        return
    }
    encoded := strings.TrimPrefix(r.URL.Path, prefix)
    // If the client included trailing slashes, reject to avoid ambiguity
    if encoded == "" || strings.Contains(encoded, "/") {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    username, err := url.PathUnescape(encoded)
    if err != nil {
        http.Error(w, "Bad Request", http.StatusBadRequest)
        return
    }

    // Validate against strict whitelist and length limit
    if !usernameRE.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Use html/template which auto-escapes content for HTML context
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    if err := helloTmpl.Execute(w, username); err != nil {
        // Avoid echoing internal errors to clients
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
}

func setSecurityHeaders(w http.ResponseWriter) {
    // Content Security Policy: no inline scripts/styles allowed, no external resources
    // Adjust if legitimate resources are needed.
    w.Header().Set("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self'; script-src 'none'; frame-ancestors 'none';")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // NOTE: HSTS must be set at TLS termination in production (only effective over HTTPS).
}

// main configures sensible timeouts and starts the server. Log startup errors.
func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/hello/", helloHandler)

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
        Handler:      nil,
        // Good practice: restrict the server to listen on a specific interface if needed.
        // For example, to bind only to localhost use "127.0.0.1:8080".
        BaseContext: func(net.Listener) context.Context { return context.Background() },
    }

    log.Printf("Starting server on %s", srv.Addr)
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}
