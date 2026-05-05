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

// Security Decision: Only allow pinging of hostnames or IPv4 addresses that match strict patterns.
// This prevents command injection (CWE-77), SSRF, and other abuses.
// No shell is invoked; exec.Command is used with arguments only.

func main() {
    // Security Decision: Listen only on localhost for demo; use HTTPS in production.
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe("127.0.0.1:8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security Decision: Only allow GET requests.
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // The URL to ping is stored as a variable "url"
    target := r.URL.Query().Get("url")
    if target == "" {
        http.Error(w, "Missing 'url' parameter", http.StatusBadRequest)
        return
    }

    // Security Decision: Validate input strictly to allow only valid hostnames or IPv4 addresses.
    if !isValidHost(target) {
        http.Error(w, "Invalid host format", http.StatusBadRequest)
        return
    }

    // Security Decision: Prevent SSRF by restricting to private IP ranges if needed.
    ip := net.ParseIP(target)
    if ip != nil && !isAllowedIP(ip) {
        http.Error(w, "IP address not allowed", http.StatusForbidden)
        return
    }

    // Security Decision: Use context with timeout to avoid hanging processes.
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()

    // Security Decision: Do not use shell; pass arguments directly.
    // Use "-n 1" (Windows) or "-c 1" (Unix) for a single ping.
    // This example assumes Windows; adjust for your OS.
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", target)

    // Security Decision: Limit output size to avoid leaking information.
    output, err := cmd.CombinedOutput()
    if err != nil {
        // Do not leak internal errors to client.
        http.Error(w, "Ping failed", http.StatusBadGateway)
        return
    }

    // Security Decision: Return only minimal, non-sensitive information.
    resp := map[string]string{
        "result": "success",
        "output": sanitizePingOutput(string(output)),
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidHost validates that the input is a valid hostname or IPv4 address.
// Hostnames: RFC 1123, max 253 chars, labels 1-63 chars, a-zA-Z0-9-.
// IPv4: 0-255.0-255.0-255.0-255
func isValidHost(host string) bool {
    // IPv4 regex
    ipv4Pattern := `^(?:\d{1,3}\.){3}\d{1,3}$`
    // Hostname regex (RFC 1123)
    hostnamePattern := `^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`
    ipv4Re := regexp.MustCompile(ipv4Pattern)
    hostnameRe := regexp.MustCompile(hostnamePattern)

    if ipv4Re.MatchString(host) {
        // Validate each octet is 0-255
        ip := net.ParseIP(host)
        return ip != nil && ip.To4() != nil
    }
    if len(host) > 253 {
        return false
    }
    return hostnameRe.MatchString(host)
}

// isAllowedIP restricts ping to non-private, non-loopback IPv4 addresses.
// Adjust as needed for your use case.
func isAllowedIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsMulticast() {
        return false
    }
    // Block private ranges (RFC1918)
    privateBlocks := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
    }
    for _, block := range privateBlocks {
        _, cidr, _ := net.ParseCIDR(block)
        if cidr.Contains(ip) {
            return false
        }
    }
    return true
}

// sanitizePingOutput returns only the first line of ping output, removing sensitive info.
func sanitizePingOutput(output string) string {
    // Security Decision: Only return the first line to avoid leaking internal network info.
    for i, c := range output {
        if c == '\n' || c == '\r' {
            return output[:i]
        }
    }
    return output
}
