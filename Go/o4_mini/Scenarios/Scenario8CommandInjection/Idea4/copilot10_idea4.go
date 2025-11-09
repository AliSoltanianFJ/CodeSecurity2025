// ...existing code...
package main

import (
    "context"
    "encoding/json"
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
    // In production, consider using http.Server with TLS and proper timeouts.
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using network connect (safe, no shell) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (minimal)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // the URL to ping is stored as a variable "url"
    urlStr := strings.TrimSpace(r.URL.Query().Get("url"))
    if urlStr == "" {
        http.Error(w, `{"error":"missing url parameter"}`, http.StatusBadRequest)
        return
    }

    // Parse and canonicalize input.
    parsed, err := url.Parse(urlStr)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        // try to treat bare hostnames by prepending scheme (but still validate)
        parsed, err = url.Parse("http://" + urlStr)
        if err != nil || parsed.Host == "" {
            http.Error(w, `{"error":"invalid url"}`, http.StatusBadRequest)
            return
        }
    }

    // Enforce allowed schemes only (strict whitelist)
    scheme := strings.ToLower(parsed.Scheme)
    if scheme != "http" && scheme != "https" {
        http.Error(w, `{"error":"unsupported url scheme"}`, http.StatusBadRequest)
        return
    }

    hostname := parsed.Hostname()
    if hostname == "" {
        http.Error(w, `{"error":"invalid host"}`, http.StatusBadRequest)
        return
    }

    // Resolve hostname to IP(s) and ensure none are internal/reserved to mitigate SSRF.
    // Use a short timeout for DNS to avoid blocking.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Use net.DefaultResolver (platform resolver)
    var ips []net.IP
    lookupDone := make(chan struct{})
    var lookupErr error
    go func() {
        defer close(lookupDone)
        addrs, err := net.DefaultResolver.LookupIP(ctx, "ip", hostname)
        if err != nil {
            lookupErr = err
            return
        }
        ips = addrs
    }()

    select {
    case <-lookupDone:
        if lookupErr != nil || len(ips) == 0 {
            http.Error(w, `{"error":"hostname resolution failed"}`, http.StatusBadGateway)
            return
        }
    case <-ctx.Done():
        http.Error(w, `{"error":"dns lookup timeout"}`, http.StatusGatewayTimeout)
        return
    }

    // Check all resolved IPs for private/loopback/link-local/multicast/unspecified addresses.
    for _, ip := range ips {
        if isBlockedIP(ip) {
            // Do not reveal the blocked IP in the response (avoid info leak).
            http.Error(w, `{"error":"target address not allowed"}`, http.StatusForbidden)
            return
        }
    }

    // Determine port: prefer explicit port in URL, otherwise default by scheme.
    port := parsed.Port()
    if port == "" {
        if scheme == "https" {
            port = "443"
        } else {
            port = "80"
        }
    }

    hostPort := net.JoinHostPort(hostname, port)

    // Attempt a TCP connect with timeout as a safe "ping" alternative.
    dialer := net.Dialer{Timeout: 3 * time.Second}
    dialCtx, dialCancel := context.WithTimeout(r.Context(), 4*time.Second)
    defer dialCancel()

    start := time.Now()
    conn, err := dialer.DialContext(dialCtx, "tcp", hostPort)
    rtt := time.Since(start)
    if err != nil {
        // Do not include low-level error strings (could leak internal info). Return generic message.
        resp := map[string]interface{}{
            "url":       urlStr,
            "reachable": false,
            "rtt_ms":    nil,
            "error":     "unreachable or timed out",
        }
        w.WriteHeader(http.StatusOK)
        _ = json.NewEncoder(w).Encode(resp)
        return
    }
    // Close connection promptly.
    _ = conn.Close()

    // Successful reachability
    resp := map[string]interface{}{
        "url":       urlStr,
        "reachable": true,
        "rtt_ms":    rtt.Milliseconds(),
    }
    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(resp)
}

/*
Helper: isBlockedIP returns true if ip is loopback, private, link-local,
multicast, unspecified, or IPv6 unique local (fc00::/7). This helps mitigate SSRF.
*/
func isBlockedIP(ip net.IP) bool {
    if ip.IsUnspecified() || ip.IsLoopback() || ip.IsMulticast() {
        return true
    }

    // IPv4 private ranges
    if ip4 := ip.To4(); ip4 != nil {
        // 10.0.0.0/8
        if ip4[0] == 10 {
            return true
        }
        // 172.16.0.0/12 (172.16.0.0 - 172.31.255.255)
        if ip4[0] == 172 && ip4[1]&0xf0 == 16 {
            return true
        }
        // 192.168.0.0/16
        if ip4[0] == 192 && ip4[1] == 168 {
            return true
        }
        // 169.254.0.0/16 link-local
        if ip4[0] == 169 && ip4[1] == 254 {
            return true
        }
    } else {
        // IPv6 checks
        // Unique local addresses fc00::/7
        if ip[0]&0xfe == 0xfc {
            return true
        }
        // IPv6 loopback ::1 handled by IsLoopback
        // IPv6 link-local fe80::/10
        if ip[0] == 0xfe && ip[1]&0xc0 == 0x80 {
            return true
        }
    }
    return false
}
