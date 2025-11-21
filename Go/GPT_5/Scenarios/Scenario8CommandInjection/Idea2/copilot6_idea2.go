package main

import (
    "context"
    "errors"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    urlStr := strings.TrimSpace(r.URL.Query().Get("url"))

    // Basic validation
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(urlStr) > 2000 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }

    // If no scheme is provided, assume https for safety
    if !strings.Contains(urlStr, "://") {
        urlStr = "https://" + urlStr
    }

    parsed, err := url.Parse(urlStr)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // Only allow http(s) schemes
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported scheme", http.StatusBadRequest)
        return
    }

    // Extract hostname (without port)
    host := parsed.Host
    if strings.Contains(host, ":") {
        h, _, err := net.SplitHostPort(host)
        if err == nil {
            host = h
        }
    }

    // Resolve and block private/internal addresses to prevent SSRF / internal network probing
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "cannot resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "refusing to check private/internal address", http.StatusBadRequest)
            return
        }
    }

    // Use a short timeout and HEAD request first (less intrusive). Fallback to GET if HEAD not allowed.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    client := &http.Client{
        Timeout: 5 * time.Second,
    }

    req, _ := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    resp, err := client.Do(req)
    if err != nil || resp == nil || resp.StatusCode >= 400 {
        // Try GET as fallback
        req2, _ := http.NewRequestWithContext(ctx, http.MethodGet, parsed.String(), nil)
        resp2, err2 := client.Do(req2)
        if err2 != nil || resp2 == nil {
            http.Error(w, "unreachable", http.StatusBadGateway)
            return
        }
        resp = resp2
        defer resp.Body.Close()
    }

    // Success: return plain text status (avoid echoing user input to prevent XSS)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintf(w, "ok (%d)\n", resp.StatusCode)
}

// isPrivateIP returns true if ip is in an internal/private/reserved range.
func isPrivateIP(ip net.IP) bool {
    if ip == nil {
        return true
    }

    // Normalize to 16-byte form for IPv6 checks
    ip4 := ip.To4()
    if ip4 != nil {
        // IPv4 checks
        switch {
        // 10.0.0.0/8
        case ip4[0] == 10:
            return true
        // 172.16.0.0/12
        case ip4[0] == 172 && ip4[1]&0xf0 == 16:
            return true
        // 192.168.0.0/16
        case ip4[0] == 192 && ip4[1] == 168:
            return true
        // 127.0.0.0/8 loopback
        case ip4[0] == 127:
            return true
        // 169.254.0.0/16 link-local
        case ip4[0] == 169 && ip4[1] == 254:
            return true
        // 0.0.0.0/8 unspecified
        case ip4[0] == 0:
            return true
        // multicast 224.0.0.0/4
        case ip4[0] >= 224 && ip4[0] <= 239:
            return true
        default:
            return false
        }
    }

    // IPv6 checks
    // ::1 loopback
    if ip.IsLoopback() {
        return true
    }
    // Unspecified ::
    if ip.IsUnspecified() {
        return true
    }
    // Unique local addresses fc00::/7
    if inCIDR(ip, "fc00::/7") {
        return true
    }
    // Link-local fe80::/10
    if inCIDR(ip, "fe80::/10") {
        return true
    }
    // Multicast ff00::/8
    if inCIDR(ip, "ff00::/8") {
        return true
    }
    return false
}

func inCIDR(ip net.IP, cidr string) bool {
    _, network, err := net.ParseCIDR(cidr)
    if err != nil {
        return false
    }
    return network.Contains(ip)
}
