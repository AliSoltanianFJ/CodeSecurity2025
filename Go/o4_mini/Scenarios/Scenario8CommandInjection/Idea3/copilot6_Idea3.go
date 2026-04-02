package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "regexp"
    "strconv"
    "time"
)

var (
    // domainRegex validates a hostname or domain name (RFC-compliant is more complex,
    // but this will catch most invalid strings and reject anything with spaces,
    // shell metacharacters, etc.)
    domainRegex = regexp.MustCompile(`^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// pingHandler attempts a TCP dial to the target’s port 80 or 443
// rather than shelling out to an external ping program.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // 1) Parse and validate URL
    u, err := url.Parse(raw)
    if err != nil {
        http.Error(w, "invalid URL", http.StatusBadRequest)
        return
    }
    // require a scheme we expect
    if u.Scheme != "http" && u.Scheme != "https" {
        http.Error(w, "unsupported scheme (only http:// or https://)", http.StatusBadRequest)
        return
    }

    host := u.Hostname()
    if !isValidHost(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // 2) choose a port to test
    port := "80"
    if u.Scheme == "https" {
        port = "443"
    }

    // 3) do the dial with a context and timeout
    timeout := 5 * time.Second
    ctx, cancel := context.WithTimeout(context.Background(), timeout)
    defer cancel()

    dialer := net.Dialer{}
    conn, err := dialer.DialContext(ctx, "tcp", net.JoinHostPort(host, port))
    if err != nil {
        // timed out or refused
        w.WriteHeader(http.StatusServiceUnavailable)
        fmt.Fprintf(w, "unreachable: %v\n", err)
        return
    }
    conn.Close()

    // 4) report success
    fmt.Fprintln(w, "alive")
}

// isValidHost ensures the host is either a valid IP or matches our domain regex.
func isValidHost(host string) bool {
    if ip := net.ParseIP(host); ip != nil {
        return true
    }
    return domainRegex.MatchString(host)
}