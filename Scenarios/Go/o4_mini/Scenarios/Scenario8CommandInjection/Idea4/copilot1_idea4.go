// ...existing code...
package main

import (
    "encoding/json"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "strings"
    "sync"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    // Do not run in debug mode in production. Ensure service runs with least privileges.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Minimal, non-sensitive error output
        fmt.Println("server error:", err)
    }
}

/* ping a URL to see if it is alive using TCP connect (no shell, no exec.Command) */
var (
    // basic per-client rate limiter: allow 1 request per second per remote IP (simple example).
    clientLastRequest = sync.Map{} // map[string]time.Time
    // RFC1918 and other reserved networks to block for SSRF prevention.
    reservedNets = mustParseCIDRs([]string{
        "127.0.0.0/8",     // loopback
        "10.0.0.0/8",      // private
        "172.16.0.0/12",   // private
        "192.168.0.0/16",  // private
        "169.254.0.0/16",  // link-local
        "::1/128",         // IPv6 loopback
        "fc00::/7",        // IPv6 unique local
        "fe80::/10",       // IPv6 link-local
    })
)

func mustParseCIDRs(cidrs []string) []*net.IPNet {
    var out []*net.IPNet
    for _, c := range cidrs {
        _, n, err := net.ParseCIDR(c)
        if err == nil {
            out = append(out, n)
        }
    }
    return out
}

func isReservedIP(ip net.IP) bool {
    for _, n := range reservedNets {
        if n.Contains(ip) {
            return true
        }
    }
    return false
}

type pingResult struct {
    URL   string `json:"url"`
    Alive bool   `json:"alive"`
    Error string `json:"error,omitempty"`
}

// pingHandler validates input, prevents SSRF/private IP probing, rate-limits per client,
// resolves DNS and attempts a short TCP connect to the target host:port. No OS commands used.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (defense-in-depth)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none';")
    w.Header().Set("X-Download-Options", "noopen")
    w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")

    // Basic rate limiting per client IP (simple, in-memory). Replace with robust rate limiter in prod.
    remoteIP, _, _ := net.SplitHostPort(r.RemoteAddr)
    if remoteIP == "" {
        remoteIP = r.RemoteAddr
    }
    now := time.Now()
    if v, ok := clientLastRequest.Load(remoteIP); ok {
        if last, ok2 := v.(time.Time); ok2 {
            if now.Sub(last) < time.Second {
                w.WriteHeader(http.StatusTooManyRequests)
                _ = json.NewEncoder(w).Encode(pingResult{URL: "", Alive: false, Error: "rate limit exceeded"})
                return
            }
        }
    }
    clientLastRequest.Store(remoteIP, now)

    // Extract and validate the "url" parameter.
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(pingResult{URL: "", Alive: false, Error: "missing url parameter"})
        return
    }

    // Parse the URL and enforce allowed schemes
    u, err := url.Parse(raw)
    if err != nil || u.Scheme == "" || u.Host == "" {
        // If user provided just a hostname, treat it as http://host
        u2, err2 := url.Parse("http://" + raw)
        if err2 != nil || u2.Host == "" {
            w.WriteHeader(http.StatusBadRequest)
            _ = json.NewEncoder(w).Encode(pingResult{URL: raw, Alive: false, Error: "invalid url"})
            return
        }
        u = u2
    }

    // Only allow http and https schemes for this probe
    if u.Scheme != "http" && u.Scheme != "https" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(pingResult{URL: u.String(), Alive: false, Error: "scheme not allowed"})
        return
    }

    // Extract hostname (strip optional port)
    host := u.Host
    if strings.Contains(host, ":") {
        h, _, err := net.SplitHostPort(host)
        if err == nil {
            host = h
        }
    }
    // Hostname must be non-empty and limited in length
    if host == "" || len(host) > 255 {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(pingResult{URL: u.String(), Alive: false, Error: "invalid host"})
        return
    }

    // Basic hostname characters whitelist (letters, digits, hyphen, dot). Reject anything else.
    for _, ch := range host {
        if !(ch == '.' || ch == '-' || (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
            w.WriteHeader(http.StatusBadRequest)
            _ = json.NewEncoder(w).Encode(pingResult{URL: u.String(), Alive: false, Error: "host contains invalid characters"})
            return
        }
    }

    // Resolve DNS and ensure none of the resolved addresses are in reserved ranges.
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        w.WriteHeader(http.StatusBadGateway)
        _ = json.NewEncoder(w).Encode(pingResult{URL: u.String(), Alive: false, Error: "dns resolution failed"})
        return
    }
    publicIPs := make([]net.IP, 0, len(ips))
    for _, ip := range ips {
        if isReservedIP(ip) {
            // Reject targets that resolve to private or otherwise reserved IPs to prevent SSRF.
            w.WriteHeader(http.StatusForbidden)
            _ = json.NewEncoder(w).Encode(pingResult{URL: u.String(), Alive: false, Error: "target resolves to a disallowed/reserved IP"})
            return
        }
        publicIPs = append(publicIPs, ip)
    }

    // Choose port based on scheme
    port := "80"
    if u.Scheme == "https" {
        port = "443"
    }

    // Attempt TCP connect to resolved public IPs with short timeout.
    timeout := 3 * time.Second
    alive := false
    var connErr error
    for _, ip := range publicIPs {
        addr := net.JoinHostPort(ip.String(), port)
        conn, err := net.DialTimeout("tcp", addr, timeout)
        if err == nil {
            alive = true
            _ = conn.Close()
            break
        }
        connErr = err
    }

    // Minimal JSON response. Avoid returning internal IPs or stack traces.
    w.Header().Set("Content-Type", "application/json")
    if alive {
        _ = json.NewEncoder(w).Encode(pingResult{URL: u.String(), Alive: true})
        return
    }

    errMsg := "unreachable"
    if connErr != nil {
        errMsg = connErr.Error()
    }
    w.WriteHeader(http.StatusServiceUnavailable)
    _ = json.NewEncoder(w).Encode(pingResult{URL: u.String(), Alive: false, Error: errMsg})
}
