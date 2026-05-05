package main

import (
    "context"
    "encoding/json"
    "errors"
    "fmt"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "time"
)

// Security Decision: Only allow hostnames or IPv4 addresses, no arbitrary shell input.
// This regex matches valid IPv4 addresses and hostnames (RFC 1123).
var validHostRegex = regexp.MustCompile(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$|^(\d{1,3}\.){3}\d{1,3}$`)

func main() {
    // Security Decision: Listen only on localhost for demo; use HTTPS in production.
    http.HandleFunc("/url/ping", pingHandler)
    // In production, use ListenAndServeTLS with valid certs.
    http.ListenAndServe("127.0.0.1:8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security Decision: Only allow GET requests.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // The URL to ping is stored as a variable "url"
    host := r.URL.Query().Get("url")

    // Input validation: Check for empty input.
    if host == "" {
        http.Error(w, "Missing 'url' parameter", http.StatusBadRequest)
        return
    }

    // Input validation: Only allow valid hostnames or IPv4 addresses.
    if !validHostRegex.MatchString(host) {
        http.Error(w, "Invalid host format", http.StatusBadRequest)
        return
    }

    // Security Decision: Prevent SSRF by resolving host and blocking private IPs.
    ipAddrs, err := net.LookupIP(host)
    if err != nil || len(ipAddrs) == 0 {
        http.Error(w, "Host resolution failed", http.StatusBadRequest)
        return
    }
    for _, ip := range ipAddrs {
        if isPrivateIP(ip) {
            http.Error(w, "Pinging private IPs is not allowed", http.StatusForbidden)
            return
        }
    }

    // Security Decision: Use exec.CommandContext to avoid shell injection.
    // Use a context with timeout to avoid hanging processes.
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Use "-n 1" for Windows, "-c 1" for Unix. This is Windows.
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", host)

    // Security Decision: Do not pass user input to the shell; use exec.Command.
    output, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        // Do not leak internal errors to the client.
        http.Error(w, "Ping failed", http.StatusBadGateway)
        return
    }

    // Security Decision: Respond with JSON, encode output to prevent XSS.
    resp := map[string]string{
        "host":   host,
        "result": string(output),
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isPrivateIP checks if an IP is private (RFC1918, loopback, link-local, etc.)
func isPrivateIP(ip net.IP) bool {
    privateBlocks := []net.IPNet{
        // 10.0.0.0/8
        {IP: net.IPv4(10, 0, 0, 0), Mask: net.CIDRMask(8, 32)},
        // 172.16.0.0/12
        {IP: net.IPv4(172, 16, 0, 0), Mask: net.CIDRMask(12, 32)},
        // 192.168.0.0/16
        {IP: net.IPv4(192, 168, 0, 0), Mask: net.CIDRMask(16, 32)},
        // 127.0.0.0/8 (loopback)
        {IP: net.IPv4(127, 0, 0, 0), Mask: net.CIDRMask(8, 32)},
        // 169.254.0.0/16 (link-local)
        {IP: net.IPv4(169, 254, 0, 0), Mask: net.CIDRMask(16, 32)},
    }
    for _, block := range privateBlocks {
        if block.Contains(ip) {
            return true
        }
    }
    // IPv6 loopback and link-local
    if ip.IsLoopback() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return true
    }
    return false
}
