package main

import (
    "fmt"
    "html"
    "net"
    "net/http"
    "net/url"
    "regexp"
    "strconv"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ping a URL to see if it is alive using a safe TCP probe (no shell/exec)
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url"
    raw := r.URL.Query().Get("url")
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Input validation
    raw = strings.TrimSpace(raw)
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(raw) > 255 {
        http.Error(w, "url parameter too long", http.StatusBadRequest)
        return
    }

    host, port, err := normalizeAndValidateHost(raw)
    if err != nil {
        // avoid echoing raw internal error details to the client
        http.Error(w, "invalid url parameter", http.StatusBadRequest)
        return
    }

    // Use a short timeout to avoid hanging connections. This is NOT an ICMP ping.
    target := net.JoinHostPort(host, port)
    d := net.Dialer{Timeout: 4 * time.Second}
    conn, err := d.Dial("tcp", target)
    if err != nil {
        // Return a safe, sanitized message
        fmt.Fprintf(w, "unreachable: %s\n", html.EscapeString(host))
        return
    }
    _ = conn.Close()
    fmt.Fprintf(w, "reachable: %s\n", html.EscapeString(host))
}

// normalizeAndValidateHost parses the provided value and returns a host and port to use.
// Accepts hostnames or IPs. If a scheme is provided it is ignored. Default port is 80.
// This function enforces strict validation to prevent injection and other abuse.
func normalizeAndValidateHost(raw string) (string, string, error) {
    // If the user provided a full URL, extract the host
    if strings.Contains(raw, "://") {
        u, err := url.Parse(raw)
        if err != nil || u.Host == "" {
            return "", "", err
        }
        // net/url may include credentials, strip them if present
        raw = u.Host
    }

    // Split host and optional port
    var hostPart, portPart string
    if strings.Contains(raw, ":") {
        // Try SplitHostPort; if it fails assume it's host without port (IPv6 corner cases)
        h, p, err := net.SplitHostPort(raw)
        if err == nil {
            hostPart = h
            portPart = p
        } else {
            // Could be an IPv6 literal without port like "[::1]" or just a hostname containing colon invalidly
            // Try to strip brackets for IPv6 literal
            hostPart = strings.Trim(raw, "[]")
            portPart = ""
        }
    } else {
        hostPart = raw
        portPart = ""
    }

    hostPart = strings.TrimSpace(hostPart)
    if hostPart == "" {
        return "", "", fmt.Errorf("empty host")
    }

    // Validate port if present
    if portPart != "" {
        p, err := strconv.Atoi(portPart)
        if err != nil || p <= 0 || p > 65535 {
            return "", "", fmt.Errorf("invalid port")
        }
    } else {
        portPart = "80"
    }

    // Validate hostname or IP
    if !isValidHostnameOrIP(hostPart) {
        return "", "", fmt.Errorf("invalid host")
    }

    return hostPart, portPart, nil
}

// isValidHostnameOrIP returns true for syntactically valid hostnames or IP addresses.
func isValidHostnameOrIP(h string) bool {
    // Accept IP addresses (v4 or v6)
    if ip := net.ParseIP(h); ip != nil {
        return true
    }

    // Hostname validation per RFC-like constraints:
    if len(h) == 0 || len(h) > 253 {
        return false
    }
    hostnameRegex := regexp.MustCompile(`^[A-Za-z0-9.-]+$`)
    if !hostnameRegex.MatchString(h) {
        return false
    }
    labels := strings.Split(h, ".")
    for _, lbl := range labels {
        if lbl == "" || len(lbl) > 63 {
            return false
        }
        // label must not start or end with hyphen
        if strings.HasPrefix(lbl, "-") || strings.HasSuffix(lbl, "-") {
            return false
        }
    }
    return true
}
// ...existing code...
