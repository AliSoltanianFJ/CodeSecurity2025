package main

import (
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
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using network connect (no shell/exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Allow only GET for this endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and validate input
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(raw) > 2048 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }

    // If scheme missing, assume http
    if !strings.Contains(raw, "://") {
        raw = "http://" + raw
    }

    parsed, err := url.Parse(raw)
    if err != nil || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // Extract hostname and determine port
    host := parsed.Hostname()
    port := parsed.Port()
    if port == "" {
        if parsed.Scheme == "https" {
            port = "443"
        } else {
            port = "80"
        }
    }

    // Resolve IPs and reject private/reserved addresses to mitigate SSRF
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        respondJSON(w, http.StatusBadGateway, map[string]interface{}{
            "target":    sanitizeTarget(parsed, port),
            "reachable": false,
            "error":     "cannot resolve host",
        })
        return
    }

    for _, ip := range ips {
        if isReservedIP(ip) {
            http.Error(w, "target not allowed", http.StatusForbidden)
            return
        }
    }

    // Attempt a TCP connection with a short timeout (no shell invocation)
    dialer := net.Dialer{Timeout: 3 * time.Second}
    conn, err := dialer.Dial("tcp", net.JoinHostPort(host, port))
    if err != nil {
        respondJSON(w, http.StatusOK, map[string]interface{}{
            "target":    sanitizeTarget(parsed, port),
            "reachable": false,
            "error":     "unreachable",
        })
        return
    }
    _ = conn.Close()

    respondJSON(w, http.StatusOK, map[string]interface{}{
        "target":    sanitizeTarget(parsed, port),
        "reachable": true,
        "error":     "",
    })
}

// respondJSON writes a JSON response with safe escaping
func respondJSON(w http.ResponseWriter, status int, v interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(v)
}

// sanitizeTarget returns a minimal, non-sensitive representation of the target.
// It strips user info and avoids returning query/fragments so sensitive data isn't exposed.
func sanitizeTarget(u *url.URL, port string) string {
    host := u.Hostname()
    // include port explicitly when non-standard or when provided
    targetHost := net.JoinHostPort(host, port)
    scheme := u.Scheme
    if scheme == "" {
        scheme = "http"
    }
    return fmt.Sprintf("%s://%s", scheme, targetHost)
}

// isReservedIP checks common private, loopback, link-local and other reserved ranges.
func isReservedIP(ip net.IP) bool {
    // Normalize to 16-byte form for IPv6 checks
    ip = ip.To16()
    if ip == nil {
        return false
    }

    // Common IPv4/IPv6 reserved ranges
    cidrs := []string{
        "127.0.0.0/8",    // loopback IPv4
        "10.0.0.0/8",     // private A
        "172.16.0.0/12",  // private B
        "192.168.0.0/16", // private C
        "169.254.0.0/16", // link local
        "::1/128",        // loopback IPv6
        "fc00::/7",       // unique local
        "fe80::/10",      // link-local IPv6
    }

    for _, cidr := range cidrs {
        _, network, err := net.ParseCIDR(cidr)
        if err == nil && network.Contains(ip) {
            return true
        }
    }
    return false
}
