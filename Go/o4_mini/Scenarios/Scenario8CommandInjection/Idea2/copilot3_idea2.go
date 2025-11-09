package main

import (
    "encoding/json"
    "errors"
    "html"
    "net"
    "net/http"
    "net/url"
    "time"
)

type pingResult struct {
    URL    string `json:"url"`
    Alive  bool   `json:"alive"`
    Status int    `json:"status,omitempty"`
    Error  string `json:"error,omitempty"`
}

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using a safe HTTP HEAD request */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // get and sanitize raw input
    raw := r.URL.Query().Get("url")
    if raw == "" {
        writeJSON(w, pingResult{Error: "missing url parameter"})
        return
    }

    // Parse and validate URL
    u, err := url.Parse(raw)
    if err != nil || u.Scheme == "" || u.Host == "" {
        writeJSON(w, pingResult{Error: "invalid url"})
        return
    }

    // Only allow http and https schemes
    if u.Scheme != "http" && u.Scheme != "https" {
        writeJSON(w, pingResult{Error: "unsupported scheme"})
        return
    }

    // Extract hostname and disallow IPs/ranges that are private/loopback/unspecified/link-local/multicast
    host := u.Hostname()
    if host == "" {
        writeJSON(w, pingResult{Error: "invalid host"})
        return
    }

    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        writeJSON(w, pingResult{Error: "host resolution failed"})
        return
    }
    for _, ip := range ips {
        if isBlockedIP(ip) {
            writeJSON(w, pingResult{Error: "host resolves to a disallowed IP"})
            return
        }
    }

    // Perform a safe HTTP HEAD with timeout (no command execution)
    client := &http.Client{
        Timeout: 5 * time.Second,
    }

    req, err := http.NewRequest(http.MethodHead, u.String(), nil)
    if err != nil {
        writeJSON(w, pingResult{Error: "failed to build request"})
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        // Return minimal, sanitized error detail
        writeJSON(w, pingResult{
            URL:   html.EscapeString(u.String()),
            Alive: false,
            Error: "request failed",
        })
        return
    }
    defer resp.Body.Close()

    alive := resp.StatusCode >= 200 && resp.StatusCode < 400

    writeJSON(w, pingResult{
        URL:    html.EscapeString(u.String()),
        Alive:  alive,
        Status: resp.StatusCode,
    })
}

func writeJSON(w http.ResponseWriter, v interface{}) {
    // Ensure no detailed internal errors leak; encoder will produce safe JSON
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(v)
}

func isBlockedIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() || ip.IsMulticast() {
        return true
    }

    // Check private IPv4 ranges and IPv6 ULA (fc00::/7)
    privateNets := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "169.254.0.0/16", // IPv4 link-local
        "127.0.0.0/8",
        "::1/128",
        "fc00::/7",
        "fe80::/10", // IPv6 link-local
    }

    for _, cidr := range privateNets {
        _, netblock, _ := net.ParseCIDR(cidr)
        if netblock != nil && netblock.Contains(ip) {
            return true
        }
    }
    return false
}
