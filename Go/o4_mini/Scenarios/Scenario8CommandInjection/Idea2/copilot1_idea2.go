package main

import (
    "net/url"
    "encoding/json"
    "fmt"
    "net"
    "net/http"
    "regexp"
    "strings"
    "errors"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using safe network checks (no shell/exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to "ping" is provided as query param "url"
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        http.Error(w, "missing 'url' parameter", http.StatusBadRequest)
        return
    }

    // Basic validation: remove surrounding angle brackets if present
    raw = strings.Trim(raw, "<>")

    // Extract hostname (and optional port) in a safe way
    host, port, err := extractHostPort(raw)
    if err != nil {
        http.Error(w, "invalid host: "+err.Error(), http.StatusBadRequest)
        return
    }

    // Resolve the host to IPs and ensure none are private/loopback/reserved
    ips, err := net.LookupIP(host)
    if err != nil {
        http.Error(w, "host resolution failed", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateOrReserved(ip) {
            http.Error(w, "refusing to probe private or reserved address", http.StatusForbidden)
            return
        }
    }

    // Attempt a TCP connect with a short timeout to the host:port
    address := net.JoinHostPort(host, port)
    timeout := 3 * time.Second
    conn, err := net.DialTimeout("tcp", address, timeout)
    status := "unreachable"
    var errMessge string
    if err == nil {
        status = "reachable"
        _ = conn.Close()
    } else {
        errMessge = err.Error()
    }

    // Return a JSON status (content-type application/json to avoid XSS via HTML)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    resp := map[string]string{
        "host":   host,
        "port":   port,
        "status": status,
    }
    if errMessge != "" {
        resp["error"] = errMessge
    }
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(resp)
}

// extractHostPort parses input and returns a validated host and port (default 443).
// It allows either a full URL or a bare hostname (optionally with port). It enforces
// hostname character rules and limits to avoid command injection and malformed hosts.
func extractHostPort(raw string) (string, string, error) {
    // If input looks like a URL, parse and extract hostname
    if strings.Contains(raw, "://") {
        pu, err := url.Parse(raw)
        if err != nil {
            return "", "", errors.New("invalid URL")
        }
        raw = pu.Host
    }

    // Remove possible userinfo
    if at := strings.LastIndex(raw, "@"); at != -1 {
        raw = raw[at+1:]
    }

    // Split host and port if provided
    host, port, err := net.SplitHostPort(raw)
    if err != nil {
        // If SplitHostPort failed, maybe no port was provided; treat raw as host
        host = raw
        port = ""
    }

    host = strings.Trim(host, "[]") // strip IPv6 brackets if present

    // Basic hostname validation
    if ip := net.ParseIP(host); ip == nil {
        // Validate as hostname label characters
        // Allow letters, digits, hyphen and dot.
        hostnameRegex := regexp.MustCompile(`^[A-Za-z0-9]([A-Za-z0-9\-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9]([A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)*$`)
        if !hostnameRegex.MatchString(host) {
            return "", "", errors.New("invalid hostname format")
        }
        // Length checks
        if len(host) > 253 {
            return "", "", errors.New("hostname too long")
        }
        // Each label <=63
        for _, label := range strings.Split(host, ".") {
            if len(label) > 63 {
                return "", "", errors.New("hostname label too long")
            }
        }
    }

    // Validate or set port
    if port == "" {
        port = "443" // prefer HTTPS port for reachability check
    } else {
        // ensure port numeric and in range
        if !regexp.MustCompile(`^[0-9]{1,5}$`).MatchString(port) {
            return "", "", errors.New("invalid port")
        }
        // numeric range
        pn := 0
        fmt.Sscanf(port, "%d", &pn)
        if pn <= 0 || pn > 65535 {
            return "", "", errors.New("port out of range")
        }
    }

    return host, port, nil
}

// isPrivateOrReserved checks common private, loopback and reserved CIDRs.
func isPrivateOrReserved(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsMulticast() {
        return true
    }
    privateCIDRs := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "127.0.0.0/8",
        "169.254.0.0/16",
        "::1/128",
        "fc00::/7",
        "fe80::/10",
    }
    for _, cidr := range privateCIDRs {
        _, n, _ := net.ParseCIDR(cidr)
        if n != nil && n.Contains(ip) {
            return true
        }
    }
    return false
}
