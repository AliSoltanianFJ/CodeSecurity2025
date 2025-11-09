package main

import (
    "context"
    "fmt"
    "html"
    "net"
    "net/http"
    "net/url"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ping a URL to see if it is alive using a safe HTTP HEAD request
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Ensure response is plain text and safe for browsers (prevents XSS in reflected values)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Only allow GET to keep handler simple and predictable
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")

    // Validate basic presence and length of input
    if urlParam == "" {
        http.Error(w, "missing 'url' parameter", http.StatusBadRequest)
        return
    }
    if len(urlParam) > 2048 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }

    // Parse and validate URL structure
    parsed, err := url.ParseRequestURI(urlParam)
    if err != nil {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }
    // Only allow http or https schemes
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported url scheme", http.StatusBadRequest)
        return
    }

    // Extract hostname (without port) and validate it
    host := parsed.Hostname()
    if host == "" {
        http.Error(w, "invalid host in url", http.StatusBadRequest)
        return
    }

    // Resolve hostname to IPs and ensure none are loopback/private (prevent SSRF to internal hosts)
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "unable to resolve host", http.StatusBadGateway)
        return
    }
    for _, ip := range ips {
        if isPrivateOrReservedIP(ip) {
            http.Error(w, "forbidden target", http.StatusForbidden)
            return
        }
    }

    // Perform a safe HTTP HEAD request with timeout and context
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }
    // Minimal, non-identifying User-Agent
    req.Header.Set("User-Agent", "MInfSc-Pinger/1.0")

    client := &http.Client{
        Timeout: 5 * time.Second,
        // Use default Transport but you could set one with stricter settings if needed
    }

    resp, err := client.Do(req)
    if err != nil {
        // Do not leak internal error details
        http.Error(w, "target not reachable", http.StatusBadGateway)
        return
    }
    defer resp.Body.Close()

    // Report sanitized result
    fmt.Fprintf(w, "URL reachable: %s (status: %s)\n", html.EscapeString(parsed.String()), html.EscapeString(resp.Status))
}

// isPrivateOrReservedIP returns true for loopback, RFC1918 private ranges, link-local, unspecified and multicast
func isPrivateOrReservedIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsMulticast() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return true
    }
    // Check IPv4 private ranges
    if ipv4 := ip.To4(); ipv4 != nil {
        // 10.0.0.0/8
        if ipv4[0] == 10 {
            return true
        }
        // 172.16.0.0/12
        if ipv4[0] == 172 && ipv4[1]&0xf0 == 16 {
            return true
        }
        // 192.168.0.0/16
        if ipv4[0] == 192 && ipv4[1] == 168 {
            return true
        }
        // 169.254.0.0/16 (link-local)
        if ipv4[0] == 169 && ipv4[1] == 254 {
            return true
        }
    }
    // IPv6 unique local addresses (fc00::/7)
    if ip.To16() != nil && ip[0]&0xfe == 0xfc {
        return true
    }
    return false
}
// ...existing code...
