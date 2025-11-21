// ...existing code...
package main

import (
    "context"
    "encoding/json"
    "errors"
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

/* ping a URL to see if it is alive using a safe TCP connect (no exec/command injection) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    type resp struct {
        Alive bool   `json:"alive"`
        Host  string `json:"host,omitempty"`
        IP    string `json:"ip,omitempty"`
        Error string `json:"error,omitempty"`
    }

    // Strict content-type for JSON responses
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Error: "missing url parameter"})
        return
    }

    // Basic length check to avoid overly long inputs
    if len(raw) > 2083 {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Error: "url parameter too long"})
        return
    }

    // Ensure we can parse it; allow bare hostnames by prefixing scheme if absent
    if !strings.Contains(raw, "://") {
        raw = "http://" + raw
    }
    parsed, err := url.Parse(raw)
    if err != nil || parsed.Host == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Error: "invalid url"})
        return
    }

    host := parsed.Hostname()
    if host == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Error: "invalid host"})
        return
    }

    // Reject suspicious characters to avoid injection-like values in host
    if strings.ContainsAny(host, "/\\ \t\r\n") {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Error: "invalid host"})
        return
    }

    // Resolve IPs for the host and block private / loopback / link-local addresses
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        w.WriteHeader(http.StatusBadGateway)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Error: "could not resolve host"})
        return
    }

    // pick the first non-private IP
    var chosenIP net.IP
    for _, ip := range ips {
        if isUnroutableIP(ip) {
            continue
        }
        chosenIP = ip
        break
    }
    if chosenIP == nil {
        w.WriteHeader(http.StatusForbidden)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Error: "resolved address is not allowed"})
        return
    }

    // Determine port from scheme (do not trust user-supplied port in Host)
    port := "80"
    if strings.EqualFold(parsed.Scheme, "https") {
        port = "443"
    }

    // Use a context with timeout and dial directly to the resolved IP (avoid re-resolving to internal addresses)
    timeout := 5 * time.Second
    ctx, cancel := context.WithTimeout(context.Background(), timeout)
    defer cancel()

    d := &net.Dialer{}
    conn, err := d.DialContext(ctx, "tcp", net.JoinHostPort(chosenIP.String(), port))
    if err != nil {
        // Do not leak internal error details
        w.WriteHeader(http.StatusServiceUnavailable)
        _ = json.NewEncoder(w).Encode(resp{Alive: false, Host: host, IP: chosenIP.String(), Error: "host unreachable"})
        return
    }
    _ = conn.Close()

    // Successful connection
    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(resp{Alive: true, Host: host, IP: chosenIP.String()})
}

// isUnroutableIP returns true for loopback, private, link-local, multicast, unspecified addresses, etc.
func isUnroutableIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsMulticast() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return true
    }

    // IPv4 private blocks and other reserved ranges
    privateCIDRs := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "127.0.0.0/8",
        "169.254.0.0/16",
        // IPv6 ULA and link-local
        "fc00::/7",
        "fe80::/10",
        "::1/128",
    }
    for _, cidr := range privateCIDRs {
        _, netblock, err := net.ParseCIDR(cidr)
        if err != nil {
            // should not happen; treat parse errors as non-blocking
            continue
        }
        if netblock.Contains(ip) {
            return true
        }
    }

    // Reject IPv4-mapped IPv6 addresses that map to private ranges by converting
    if ip4 := ip.To4(); ip4 != nil {
        // additional checks could go here if needed
        _ = ip4
    }

    // If we reach here, assume it's routable
    return false
}
```// filepath:
