// ...existing code...
package main

import (
    "context"
    "fmt"
    "io"
    "net"
    "net/http"
    "net/url"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using a safe HTTP probe (no shell/exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlStr := strings.TrimSpace(r.URL.Query().Get("url"))

    // Basic validation
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(urlStr) > 2083 { // conservative max length
        http.Error(w, "url parameter too long", http.StatusBadRequest)
        return
    }

    // If scheme missing, assume http
    if !strings.Contains(urlStr, "://") {
        urlStr = "http://" + urlStr
    }

    u, err := url.ParseRequestURI(urlStr)
    if err != nil {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // Only allow http(s) schemes
    if u.Scheme != "http" && u.Scheme != "https" {
        http.Error(w, "unsupported url scheme", http.StatusBadRequest)
        return
    }

    host := u.Hostname()
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Resolve DNS and reject private / loopback / unspecified addresses to prevent SSRF
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "unable to resolve host", http.StatusBadRequest)
        return
    }
    if containsPrivateOrReservedIP(ips) {
        http.Error(w, "host resolves to a private or reserved IP", http.StatusForbidden)
        return
    }

    // Probe the endpoint with a short timeout using HEAD first, fallback to GET
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    client := &http.Client{
        Timeout: 5 * time.Second,
    }

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, u.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        // fallback to GET in case server doesn't support HEAD
        reqGet, err2 := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
        if err2 != nil {
            http.Error(w, "unreachable", http.StatusServiceUnavailable)
            return
        }
        resp, err = client.Do(reqGet)
        if err != nil {
            http.Error(w, "unreachable", http.StatusServiceUnavailable)
            return
        }
    }
    defer resp.Body.Close()

    // Read and discard a small amount of body to allow connection reuse, but limit size
    _, _ = io.CopyN(io.Discard, resp.Body, 1<<10) // discard up to 1KB

    // Respond with safe, minimal info (avoid echoing user input or internal details)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if resp.StatusCode >= 200 && resp.StatusCode < 400 {
        fmt.Fprintf(w, "reachable (HTTP %d)\n", resp.StatusCode)
    } else {
        fmt.Fprintf(w, "unreachable (HTTP %d)\n", resp.StatusCode)
    }
}

// Helper: check whether any IP is private, loopback, unspecified, or link-local.
func containsPrivateOrReservedIP(ips []net.IP) bool {
    privateNets := []*net.IPNet{}
    for _, cidr := range []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "169.254.0.0/16", // link-local IPv4
        "127.0.0.0/8",    // loopback IPv4
        "::1/128",        // loopback IPv6
        "fc00::/7",       // unique local IPv6
        "fe80::/10",      // link-local IPv6
        "::/128",         // unspecified
    } {
        _, netw, err := net.ParseCIDR(cidr)
        if err == nil {
            privateNets = append(privateNets, netw)
        }
    }

    for _, ip := range ips {
        if ip.IsUnspecified() || ip.IsLoopback() {
            return true
        }
        for _, netw := range privateNets {
            if netw.Contains(ip) {
                return true
            }
        }
    }
    return false
}
// ...existing code...
