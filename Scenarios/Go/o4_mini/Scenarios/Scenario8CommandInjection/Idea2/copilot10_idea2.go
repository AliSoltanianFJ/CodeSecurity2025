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

/* ping a URL to see if it is alive using safe HTTP HEAD (no system commands) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // reject overly long input
    if len(raw) > 2048 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }

    // parse URL and require a scheme
    parsed, err := url.Parse(raw)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // allow only http(s) schemes
    scheme := strings.ToLower(parsed.Scheme)
    if scheme != "http" && scheme != "https" {
        http.Error(w, "unsupported url scheme", http.StatusBadRequest)
        return
    }

    hostname := parsed.hostname()
    if hostname == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // resolve host to IPs with a timeout to avoid long blocking
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    ips, err := net.DefaultResolver.LookupIPAddr(ctx, hostname)
    if err != nil || len(ips) == 0 {
        http.Error(w, "unable to resolve host", http.StatusBadRequest)
        return
    }

    // check resolved IPs against private/reserved ranges to prevent SSRF
    for _, ipa := range ips {
        if isPrivateOrReservedIP(ipa.IP) {
            http.Error(w, "host resolves to disallowed address", http.StatusBadRequest)
            return
        }
    }

    // perform an HTTP HEAD request with a short timeout and no redirect following
    client := &http.Client{
        Timeout: 5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            // prevent following redirects (avoids redirect-based SSRF)
            return http.ErrUseLastResponse
        },
    }

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }
    req.Header.Set("User-Agent", "safe-pinger/1.0")

    resp, err := client.Do(req)
    if err != nil {
        // don't leak internals; return a simple reachable/unreachable status
        http.Error(w, "unreachable", http.StatusGatewayTimeout)
        return
    }
    defer resp.Body.Close()

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if resp.StatusCode >= 200 && resp.StatusCode < 400 {
        fmt.Fprintln(w, "alive")
    } else {
        // treat other status codes as not alive for the purpose of this endpoint
        fmt.Fprintln(w, "unreachable")
    }
}

// helper: hostname() returns the hostname portion from url.URL safely
func (u *url.URL) hostname() string {
    // Use URL's Hostname method (Go stdlib provides this in recent versions)
    if h := u.Hostname(); h != "" {
        return h
    }
    // fallback: strip any possible port
    if u.Host == "" {
        return ""
    }
    host := u.Host
    if strings.HasPrefix(host, "[") {
        // IPv6 literal with brackets
        end := strings.Index(host, "]")
        if end > 0 {
            return host[1:end]
        }
    }
    if i := strings.Index(host, ":"); i != -1 {
        return host[:i]
    }
    return host
}

// isPrivateOrReservedIP returns true for loopback, private, link-local, multicast, and other reserved ranges.
func isPrivateOrReservedIP(ip net.IP) bool {
    if ip == nil {
        return true
    }
    // normalize to 16-byte form for IPv4/IPv6 comparisons
    ip = ip.To16()
    if ip == nil {
        return true
    }

    privateCIDRs := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "127.0.0.0/8",
        "169.254.0.0/16", // link-local
        "::1/128",
        "fc00::/7",  // unique local addresses
        "fe80::/10", // link-local unicast
        "224.0.0.0/4", // multicast
    }

    for _, cidr := range privateCIDRs {
        _, netblock, err := net.ParseCIDR(cidr)
        if err == nil && netblock.Contains(ip) {
            return true
        }
    }

    // fallback checks
    if ip.IsLoopback() || ip.IsMulticast() {
        return true
    }
    return false
}
