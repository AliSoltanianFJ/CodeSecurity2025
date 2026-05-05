package main

import (
    "context"
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
/* ping a URL to see if it is alive using a safe HTTP probe (no shell/external commands) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")

    // Basic validation: present and reasonable length
    if urlParam == "" || len(urlParam) > 2048 {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // Parse and validate URL
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // Only allow http and https schemes
    scheme := strings.ToLower(parsed.Scheme)
    if scheme != "http" && scheme != "https" {
        http.Error(w, "unsupported scheme", http.StatusBadRequest)
        return
    }

    // Extract hostname for DNS resolution and checks
    host := parsed.Hostname()
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Resolve host to IP(s) and deny private/local addresses to avoid SSRF to internal services
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "cannot resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "refused (private or local address)", http.StatusBadRequest)
            return
        }
    }

    // Perform a safe HTTP HEAD probe with a timeout and without following redirects
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    client := &http.Client{
        Timeout:       5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error { return http.ErrUseLastResponse },
        Transport: &http.Transport{
            Proxy:                 http.ProxyFromEnvironment,
            TLSHandshakeTimeout:   3 * time.Second,
            ResponseHeaderTimeout: 3 * time.Second,
            DisableKeepAlives:     true,
        },
    }

    resp, err := client.Do(req)
    if err != nil {
        http.Error(w, "unreachable", http.StatusBadGateway)
        return
    }
    defer resp.Body.Close()

    // Return only minimal, safe information (no reflection of raw user input)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if resp.StatusCode >= 200 && resp.StatusCode < 400 {
        fmt.Fprintln(w, "reachable")
    } else {
        fmt.Fprintln(w, "unreachable")
    }
}

// isPrivateIP returns true for loopback, link-local and private RFC1918/4291 address ranges.
func isPrivateIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return true
    }

    privateNets := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "169.254.0.0/16",
        "127.0.0.0/8",
        "::1/128",
        "fc00::/7", // unique local addresses (IPv6)
        "fe80::/10",
    }

    for _, cidr := range privateNets {
        _, netw, err := net.ParseCIDR(cidr)
        if err == nil && netw.Contains(ip) {
            return true
        }
    }
    return false
}
