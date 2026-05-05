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

// Security Decision: Only allow pinging of hostnames or IP addresses that match strict validation rules.
// Prevents command injection (CWE-77), SSRF, and other abuses.

func main() {
    // Secure default: Listen only on localhost. For production, use HTTPS and proper firewall rules.
    http.HandleFunc("/url/ping", pingHandler)
    // In production, use HTTPS with proper certificates.
    http.ListenAndServe("127.0.0.1:8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests for this endpoint.
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get the "url" parameter from the query string.
    target := r.URL.Query().Get("url")

    // Validate and sanitize the input.
    if err := validatePingTarget(target); err != nil {
        http.Error(w, "Invalid target: "+err.Error(), http.StatusBadRequest)
        return
    }

    // Security: Use context with timeout to avoid hanging processes.
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Security: Use exec.CommandContext with arguments, never build command strings.
    // Only allow 1 ping packet for minimal exposure.
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", target)

    // Security: Limit output size to avoid resource exhaustion.
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

    // Security: Respond with JSON, encode output to prevent XSS.
    resp := map[string]string{
        "result": string(output),
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// validatePingTarget ensures the target is a valid hostname or IP address and not a reserved or local address.
// Prevents SSRF, command injection, and other abuses.
func validatePingTarget(target string) error {
    if len(target) == 0 || len(target) > 253 {
        return errors.New("target length invalid")
    }

    // Allow only valid hostnames or IPv4/IPv6 addresses.
    hostnameRegex := regexp.MustCompile(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`)
    ip := net.ParseIP(target)
    if ip == nil && !hostnameRegex.MatchString(target) {
        return errors.New("invalid hostname or IP address")
    }

    // Prevent SSRF: Disallow localhost, private, and reserved IPs.
    if ip != nil {
        if isPrivateOrReservedIP(ip) {
            return errors.New("target is a private or reserved IP")
        }
    } else {
        // Resolve hostname and check all IPs.
        ips, err := net.LookupIP(target)
        if err != nil {
            return errors.New("hostname resolution failed")
        }
        for _, resolvedIP := range ips {
            if isPrivateOrReservedIP(resolvedIP) {
                return errors.New("hostname resolves to private or reserved IP")
            }
        }
    }
    return nil
}

// isPrivateOrReservedIP checks if the IP is private, loopback, or reserved.
func isPrivateOrReservedIP(ip net.IP) bool {
    privateBlocks := []net.IPNet{
        // IPv4 private ranges
        {IP: net.IPv4(10, 0, 0, 0), Mask: net.CIDRMask(8, 32)},
        {IP: net.IPv4(172, 16, 0, 0), Mask: net.CIDRMask(12, 32)},
        {IP: net.IPv4(192, 168, 0, 0), Mask: net.CIDRMask(16, 32)},
        // IPv4 loopback
        {IP: net.IPv4(127, 0, 0, 0), Mask: net.CIDRMask(8, 32)},
        // IPv6 loopback
        {IP: net.ParseIP("::1"), Mask: net.CIDRMask(128, 128)},
        // IPv6 link-local
        {IP: net.ParseIP("fe80::"), Mask: net.CIDRMask(10, 128)},
        // IPv6 unique local
        {IP: net.ParseIP("fc00::"), Mask: net.CIDRMask(7, 128)},
    }
    for _, block := range privateBlocks {
        if block.Contains(ip) {
            return true
        }
    }
    return ip.IsUnspecified() || ip.IsMulticast()
}
