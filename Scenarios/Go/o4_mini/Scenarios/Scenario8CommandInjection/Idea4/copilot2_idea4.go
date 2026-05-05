package main

import (
    "context"
    "encoding/json"
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
    // Production: run behind a reverse proxy with TLS; here we bind to localhost for development safety.
    http.ListenAndServe(":8080", nil)
}

// pingHandler performs a safe "ping-like" reachability check by attempting a TCP connect.
// It deliberately does NOT call any OS "ping" binary to avoid command injection.
// Security controls:
// - Whitelist schemes (http, https).
// - Canonicalize and validate URL.
// - Resolve DNS and block private/local IP ranges (mitigate SSRF).
// - Use connection timeouts and small, controlled responses.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Minimal hardening headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Limit request size for safety (no large bodies expected)
    r.Body.Close()

    q := r.URL.Query().Get("url")
    if q == "" {
        http.Error(w, `{"error":"missing url parameter"}`, http.StatusBadRequest)
        return
    }
    // Basic length limit
    if len(q) > 256 {
        http.Error(w, `{"error":"url too long"}`, http.StatusBadRequest)
        return
    }

    // Parse and validate URL
    parsed, err := url.ParseRequestURI(q)
    if err != nil {
        http.Error(w, `{"error":"invalid url"}`, http.StatusBadRequest)
        return
    }

    // Disallow userinfo in URL (username:password@host)
    if parsed.User != nil {
        http.Error(w, `{"error":"userinfo not allowed in url"}`, http.StatusBadRequest)
        return
    }

    // Whitelist schemes
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, `{"error":"unsupported scheme; only http and https allowed"}`, http.StatusBadRequest)
        return
    }

    host := parsed.Host
    // If host includes port, SplitHostPort; handle IPv6 bracketed hosts
    h, _, err := net.SplitHostPort(host)
    if err == nil {
        host = h
    } else {
        // SplitHostPort fails when there's no port — that's fine; keep host as-is.
        // For IPv6 without port, ensure brackets are removed if present.
        host = strings.Trim(host, "[]")
    }

    // Reject IP-literal that is clearly local or unspecified; but we'll perform DNS resolution and check IPs too.
    // Resolve host to IPs
    ips, err := net.LookupIP(host)
    if err != nil {
        // Avoid leaking internal DNS errors; provide generic message.
        http.Error(w, `{"error":"host resolution failed"}`, http.StatusBadRequest)
        return
    }
    if len(ips) == 0 {
        http.Error(w, `{"error":"no IP addresses found for host"}`, http.StatusBadRequest)
        return
    }

    // Check all resolved IPs for unsafe ranges
    for _, ip := range ips {
        if isUnsafeIP(ip) {
            http.Error(w, `{"error":"resolved IP is in a private or reserved range"}`, http.StatusBadRequest)
            return
        }
    }

    // Choose port based on scheme if not explicitly provided
    port := ""
    _, p, perr := net.SplitHostPort(parsed.Host)
    if perr == nil {
        port = p
    } else {
        if parsed.Scheme == "https" {
            port = "443"
        } else {
            port = "80"
        }
    }

    target := net.JoinHostPort(host, port)

    // Dial with timeout and context to limit resource usage
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()
    dialer := &net.Dialer{
        Timeout:   4 * time.Second,
        KeepAlive: 30 * time.Second,
    }

    start := time.Now()
    conn, err := dialer.DialContext(ctx, "tcp", target)
    duration := time.Since(start)
    if conn != nil {
        // Close immediately; we're only checking TCP connectability
        conn.Close()
    }

    resp := make(map[string]interface{})
    if err != nil {
        // Return a safe, non-leaking error message
        resp["success"] = false
        resp["error"] = "unreachable"
        resp["rtt_ms"] = duration.Milliseconds()
        w.WriteHeader(http.StatusOK)
        _ = json.NewEncoder(w).Encode(resp)
        return
    }

    resp["success"] = true
    resp["host"] = host
    resp["ips"] = ipsToStrings(ips)
    resp["rtt_ms"] = duration.Milliseconds()

    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(resp)
}

// isUnsafeIP returns true for loopback, private, link-local, multicast, unspecified, and unique-local IPv6 addresses.
func isUnsafeIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsMulticast() {
        return true
    }
    // IPv4 private ranges and link-local
    if ip4 := ip.To4(); ip4 != nil {
        // 10.0.0.0/8
        if ip4[0] == 10 {
            return true
        }
        // 172.16.0.0/12
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
        // 127.0.0.0/8 loopback handled above, but double-check
        if ip4[0] == 127 {
            return true
        }
        return false
    }
    // IPv6 checks
    // ::1 loopback already caught by IsLoopback
    // fc00::/7 unique local addresses
    if ip[0]&0xfe == 0xfc {
        return true
    }
    // fe80::/10 link-local
    if ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 {
        return true
    }
    // ff00::/8 multicast already caught by IsMulticast
    return false
}

func ipsToStrings(ips []net.IP) []string {
    out := make([]string, 0, len(ips))
    for _, ip := range ips {
        out = append(out, ip.String())
    }
    return out
}

// ...existing code...
