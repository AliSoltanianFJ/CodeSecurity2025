package main

import (
    "context"
    "errors"
    "fmt"
    "io"
    "net"
    "net/http"
    "net/url"
    "strings"
    "time"
)

// ...existing code...

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    // Listen only on localhost by default for safety; change bind only when you harden network ACLs.
    http.ListenAndServe("127.0.0.1:8080", nil)
}

/* ping a URL to see if it is alive using a safe HTTP probe (no shell). */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Basic security response headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // 1) Input: get and validate the "url" parameter
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        http.Error(w, "missing 'url' parameter", http.StatusBadRequest)
        return
    }
    if len(raw) > 2083 { // reasonable maximum
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }

    // 2) Parse and canonicalize URL
    u, err := url.Parse(raw)
    if err != nil || u.Scheme == "" || u.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }
    // Only allow http(s)
    if u.Scheme != "http" && u.Scheme != "https" {
        http.Error(w, "only http/https schemes are allowed", http.StatusBadRequest)
        return
    }

    // 3) Resolve hostname and block private/reserved IP ranges (SSRF mitigation)
    host := u.Hostname()
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "cannot resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "host resolves to a private or disallowed IP", http.StatusForbidden)
            return
        }
    }

    // 4) Create a guarded HTTP client with timeouts and redirect validation
    timeout := 5 * time.Second
    ctx, cancel := context.WithTimeout(r.Context(), timeout)
    defer cancel()

    client := &http.Client{
        Timeout: timeout,
        Transport: &http.Transport{
            DisableKeepAlives:   true,
            TLSHandshakeTimeout: 3 * time.Second,
            DialContext: (&net.Dialer{
                Timeout:   3 * time.Second,
                KeepAlive: 0,
            }).DialContext,
        },
        // Validate redirects: deny redirect to private IPs and limit count.
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            if len(via) >= 3 {
                return errors.New("too many redirects")
            }
            h := req.URL.Hostname()
            ips, err := net.LookupIP(h)
            if err != nil {
                return errors.New("redirect to unresolved host blocked")
            }
            for _, ip := range ips {
                if isPrivateIP(ip) {
                    return errors.New("redirect to private IP blocked")
                }
            }
            return nil
        },
    }

    // 5) Try HEAD first (lightweight). If HEAD fails with 405, fall back to GET but limit body.
    req, err := http.NewRequestWithContext(ctx, "HEAD", u.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        // If HEAD not allowed, try GET with limited body consumption
        if urlErr, ok := err.(*url.Error); ok && strings.Contains(urlErr.Err.Error(), "method") {
            // fallthrough to GET below
        } else {
            http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusBadGateway)
            return
        }
    }

    if resp != nil && resp.Body != nil {
        defer resp.Body.Close()
    }

    // If HEAD returned, evaluate status
    if resp != nil {
        if resp.StatusCode < 400 {
            fmt.Fprintf(w, "alive (%d)\n", resp.StatusCode)
            return
        }
        // treat >=400 as unreachable / error
        fmt.Fprintf(w, "unreachable (%d)\n", resp.StatusCode)
        return
    }

    // Fallback to GET (limit body read to 1MB to avoid DoS)
    req2, err := http.NewRequestWithContext(ctx, "GET", u.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }
    resp2, err := client.Do(req2)
    if err != nil {
        http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusBadGateway)
        return
    }
    defer resp2.Body.Close()

    // Drain up to 1MB and discard — prevents large-body DoS
    _, _ = io.CopyN(io.Discard, resp2.Body, 1024*1024)

    if resp2.StatusCode < 400 {
        fmt.Fprintf(w, "alive (%d)\n", resp2.StatusCode)
        return
    }
    fmt.Fprintf(w, "unreachable (%d)\n", resp2.StatusCode)
}

// isPrivateIP returns true for loopback, link-local, private (RFC1918), multicast, and ULA ranges.
// Blocking these helps mitigate SSRF to internal services.
func isPrivateIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() {
        return true
    }
    if ip4 := ip.To4(); ip4 != nil {
        // 10.0.0.0/8
        if ip4[0] == 10 {
            return true
        }
        // 172.16.0.0/12
        if ip4[0] == 172 && ip4[1] >= 16 && ip4[1] <= 31 {
            return true
        }
        // 192.168.0.0/16
        if ip4[0] == 192 && ip4[1] == 168 {
            return true
        }
        // 169.254.0.0/16 link-local
        if ip4[0] == 169 && ip4[1] == 254 {
            return true
        }
        // 224.0.0.0/4 multicast and beyond (includes experimental)
        if ip4[0] >= 224 {
            return true
        }
        return false
    }
    // IPv6 checks
    // ::1 loopback
    if ip.IsLoopback() {
        return true
    }
    // fc00::/7 Unique Local Address
    if len(ip) == net.IPv6len && (ip[0]&0xfe) == 0xfc {
        return true
    }
    // fe80::/10 link-local
    if len(ip) == net.IPv6len && ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 {
        return true
    }
    // multicast ff00::/8
    if len(ip) == net.IPv6len && ip[0] == 0xff {
        return true
    }
    return false
}
