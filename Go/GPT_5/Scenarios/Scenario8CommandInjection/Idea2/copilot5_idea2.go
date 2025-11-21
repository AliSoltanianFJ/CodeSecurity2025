// ...existing code...
package main

import (
    "encoding/json"
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

// ...existing code...
/* ping a URL to see if it is alive using a safe HTTP probe (no shell exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" {
        http.Error(w, "missing 'url' parameter", http.StatusBadRequest)
        return
    }

    // Parse and validate URL
    parsed, err := url.ParseRequestURI(urlStr)
    if err != nil {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported URL scheme", http.StatusBadRequest)
        return
    }
    host := parsed.Hostname()
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Resolve DNS and reject private / loopback / link-local addresses to mitigate SSRF
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "cannot resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateOrUnroutable(ip) {
            http.Error(w, "resolved address is not allowed", http.StatusBadRequest)
            return
        }
    }

    // Perform a safe HEAD probe with timeout and no redirects
    client := &http.Client{
        Timeout: 5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            // prevent following redirects to other hosts
            return http.ErrUseLastResponse
        },
    }

    req, err := http.NewRequest("HEAD", parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }
    req.Header.Set("User-Agent", "safe-url-pinger/1.0")

    resp, err := client.Do(req)
    if err != nil {
        // Some servers may not accept HEAD; try GET as a fallback (still safe and contained by timeout)
        reqGet, err2 := http.NewRequest("GET", parsed.String(), nil)
        if err2 == nil {
            reqGet.Header.Set("User-Agent", "safe-url-pinger/1.0")
            resp, err = client.Do(reqGet)
        }
    }

    type result struct {
        URL       string `json:"url"`
        Reachable bool   `json:"reachable"`
        Status    int    `json:"status,omitempty"`
        Error     string `json:"error,omitempty"`
    }

    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    if err != nil {
        _ = json.NewEncoder(w).Encode(result{
            URL:       parsed.String(),
            Reachable: false,
            Error:     "request failed",
        })
        return
    }
    defer resp.Body.Close()

    // Consider HTTP status codes < 400 as reachable
    reachable := resp.StatusCode < 400

    _ = json.NewEncoder(w).Encode(result{
        URL:       parsed.String(),
        Reachable: reachable,
        Status:    resp.StatusCode,
    })
}

// ...existing code...
// isPrivateOrUnroutable returns true for loopback, private, link-local, unspecified, multicast and other non-global addresses.
func isPrivateOrUnroutable(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsMulticast() {
        return true
    }
    if ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return true
    }

    // IPv4 private ranges and other special ranges
    privateCIDRs := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "169.254.0.0/16", // IPv4 link-local
        "127.0.0.0/8",    // loopback
        "::1/128",        // IPv6 loopback
        "fe80::/10",      // IPv6 link-local
        "fc00::/7",       // IPv6 unique local
    }

    for _, cidr := range privateCIDRs {
        _, netblock, _ := net.ParseCIDR(cidr)
        if netblock != nil && netblock.Contains(ip) {
            return true
        }
    }

    // If it's not global unicast, treat as unroutable
    if !ip.IsGlobalUnicast() {
        return true
    }

    return false
}
