// ...existing code...
package main

import (
    "context"
    "encoding/json"
    "fmt"
    neturl "net/url"
    "net"
    "net/http"
    "regexp"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    // Listen only on a specific interface if possible; default here is all interfaces.
    // In production, run behind a reverse proxy and enforce TLS (ListenAndServeTLS).
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using a safe TCP check (no shell). */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers
    w.Header().Set("Content-Type", "application/json")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Only allow GET
    if r.Method != http.MethodGet {
        http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
        return
    }

    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        http.Error(w, `{"error":"missing url parameter"}`, http.StatusBadRequest)
        return
    }

    // Parse and validate URL
    u, err := neturl.Parse(raw)
    if err != nil || u.Scheme == "" || u.Host == "" {
        http.Error(w, `{"error":"invalid url"}`, http.StatusBadRequest)
        return
    }

    // Allow only http and https schemes for this probe
    switch strings.ToLower(u.Scheme) {
    case "http", "https":
    default:
        http.Error(w, `{"error":"unsupported scheme"}`, http.StatusBadRequest)
        return
    }

    host := u.Hostname()
    port := u.Port()
    if port == "" {
        if u.Scheme == "https" {
            port = "443"
        } else {
            port = "80"
        }
    }

    // Validate hostname syntax or IP
    if !isValidHost(host) {
        http.Error(w, `{"error":"invalid hostname"}`, http.StatusBadRequest)
        return
    }

    // Resolve DNS and ensure resolved IPs are not in private/internal ranges
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, `{"error":"dns resolution failed"}`, http.StatusBadGateway)
        return
    }

    // Pick the first public IP and ensure none of the resolved addresses are private.
    var candidateIP net.IP
    for _, ip := range ips {
        if isPrivateIP(ip) {
            // Reject if any resolved IP is private/loopback/link-local to prevent SSRF to internal addresses.
            http.Error(w, `{"error":"resolved to private or link-local address; request rejected"}`, http.StatusBadRequest)
            return
        }
        // prefer IPv4 but accept IPv6
        if candidateIP == nil {
            candidateIP = ip
        }
    }

    if candidateIP == nil {
        http.Error(w, `{"error":"no suitable public address found"}`, http.StatusBadGateway)
        return
    }

    // Dial the resolved IP directly to avoid race between DNS resolve and connect to different IPs.
    addr := net.JoinHostPort(candidateIP.String(), port)
    dialer := net.Dialer{}
    ctx, cancel := context.WithTimeout(r.Context(), 4*time.Second)
    defer cancel()

    start := time.Now()
    conn, err := dialer.DialContext(ctx, "tcp", addr)
    latency := time.Since(start)
    if err != nil {
        // Return a safe message
        resp := map[string]interface{}{
            "host":      host,
            "ip":        candidateIP.String(),
            "port":      port,
            "reachable": false,
            "error":     "connect failed",
            "latencyMs": latency.Milliseconds(),
        }
        _ = json.NewEncoder(w).Encode(resp)
        return
    }
    _ = conn.Close()

    resp := map[string]interface{}{
        "host":      host,
        "ip":        candidateIP.String(),
        "port":      port,
        "reachable": true,
        "latencyMs": latency.Milliseconds(),
    }
    _ = json.NewEncoder(w).Encode(resp)
}

/* validate hostnames strictly: allow IP addresses or RFC-compliant hostnames */
func isValidHost(h string) bool {
    // If it's a valid IP, accept (will be checked for private ranges later)
    if ip := net.ParseIP(h); ip != nil {
        return true
    }
    // Hostname regex: labels 1-63 chars, overall length <=253, allowed a-z0-9- and dot.
    // This is a strict whitelist to reduce injection/SSRF risk.
    const hostPattern = `^(?i:[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?)*)$`
    re := regexp.MustCompile(hostPattern)
    if len(h) > 253 {
        return false
    }
    return re.MatchString(h)
}

/* detect private / loopback / link-local addresses */
func isPrivateIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return true
    }
    // IPv4 private ranges
    if ipv4 := ip.To4(); ipv4 != nil {
        b0 := ipv4[0]
        b1 := ipv4[1]
        switch {
        case b0 == 10:
            return true // 10.0.0.0/8
        case b0 == 172 && b1 >= 16 && b1 <= 31:
            return true // 172.16.0.0/12
        case b0 == 192 && b1 == 168:
            return true // 192.168.0.0/16
        case b0 == 127:
            return true // 127.0.0.0/8 loopback
        case b0 == 169 && b1 == 254:
            return true // 169.254.0.0/16 link-local
        default:
            return false
        }
    }
    // IPv6 checks: loopback, unique local (fc00::/7), link-local (fe80::/10)
    if ip.To16() != nil {
        // unique local fc00::/7 => first 7 bits 1111110 -> check prefix fc00/7
        if len(ip) >= 2 {
            if ip[0]&0xfe == 0xfc {
                return true // fc00::/7
            }
            // fe80::/10 link-local: first 10 bits 1111111010 (0xfe80)
            if ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 {
                return true
            }
        }
    }
    return false
}
