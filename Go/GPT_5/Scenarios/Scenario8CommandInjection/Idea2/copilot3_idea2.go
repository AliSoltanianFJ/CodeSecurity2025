package main

import (
    "context"
    "encoding/json"
    "errors"
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
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using an HTTP request (safe, no exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    type resp struct {
        URL        string `json:"url"`
        Reachable  bool   `json:"reachable"`
        StatusCode int    `json:"status_code,omitempty"`
        Message    string `json:"message,omitempty"`
    }

    // Ensure JSON response and safe encoding
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Get and sanitize input
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Message: "missing url parameter"})
        return
    }
    if len(raw) > 2048 {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Message: "url too long"})
        return
    }

    // Parse URL and require explicit http/https scheme
    u, err := url.Parse(raw)
    if err != nil || u.Scheme == "" || (u.Scheme != "http" && u.Scheme != "https") {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Message: "invalid or unsupported url; require http or https scheme"})
        return
    }
    if u.Host == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Message: "url missing host"})
        return
    }

    // Extract host without port
    host := u.Host
    if h, _, err := net.SplitHostPort(u.Host); err == nil {
        host = h
    }

    // Resolve and ensure target host does not resolve to private or loopback addresses (defense against SSRF)
    ips, err := net.LookupIP(host)
    if err != nil {
        // If DNS resolution fails treat as unreachable but don't leak internal details
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{URL: raw, Message: "unable to resolve host"})
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            w.WriteHeader(http.StatusForbidden)
            _ = json.NewEncoder(w).Encode(resp{Message: "requests to internal or private IP ranges are not allowed"})
            return
        }
    }

    // Create a request with a short timeout and no redirects
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, u.String(), nil)
    if err != nil {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Message: "failed to create request"})
        return
    }

    client := &http.Client{
        Timeout:       5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error { return http.ErrUseLastResponse },
    }

    // Try HEAD first; if server forbids, fall back to GET but limit body read.
    httpResp, err := client.Do(req)
    if err != nil {
        // Try GET as fallback for servers that do not support HEAD
        reqGet, rerr := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
        if rerr != nil {
            w.WriteHeader(http.StatusServiceUnavailable)
            _ = json.NewEncoder(w).Encode(resp{URL: raw, Message: "unreachable"})
            return
        }
        httpResp, err = client.Do(reqGet)
        if err != nil {
            w.WriteHeader(http.StatusServiceUnavailable)
            _ = json.NewEncoder(w).Encode(resp{URL: raw, Message: "unreachable"})
            return
        }
    }
    defer httpResp.Body.Close()

    // Limit body read to avoid large downloads (defense in depth)
    _, _ = io.Copy(io.Discard, io.LimitReader(httpResp.Body, 1024))

    // Consider reachable if status code < 400
    reachable := httpResp.StatusCode < 400

    _ = json.NewEncoder(w).Encode(resp{
        URL:        raw,
        Reachable:  reachable,
        StatusCode: httpResp.StatusCode,
    })
}

// isPrivateIP returns true for loopback, link-local, and RFC1918/fc00:: private addresses.
func isPrivateIP(ip net.IP) bool {
    privateCIDRs := []string{
        "127.0.0.0/8",    // loopback
        "10.0.0.0/8",     // RFC1918
        "172.16.0.0/12",  // RFC1918
        "192.168.0.0/16", // RFC1918
        "169.254.0.0/16", // link-local
        "::1/128",        // IPv6 loopback
        "fe80::/10",      // IPv6 link-local
        "fc00::/7",       // IPv6 unique local
    }
    for _, cidr := range privateCIDRs {
        _, netw, err := net.ParseCIDR(cidr)
        if err != nil {
            continue
        }
        if netw.Contains(ip) {
            return true
        }
    }
    // Additionally treat unspecified or multicast addresses as private/unwanted
    if ip.IsUnspecified() || ip.IsMulticast() {
        return true
    }
    return false
}

// ...existing code...
