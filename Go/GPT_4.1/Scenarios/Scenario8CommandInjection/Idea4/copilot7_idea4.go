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

// Security Decision: Only allow hostnames or IPv4 addresses, no ports, no special chars.
// This regex matches valid IPv4 addresses or hostnames (letters, digits, hyphens, dots).
var allowedHost = regexp.MustCompile(`^([a-zA-Z0-9\-\.]{1,253})$`)

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
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // The URL to ping is stored as a variable "url"
    host := r.URL.Query().Get("url")
    if !isValidHost(host) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Security Decision: Prevent SSRF by resolving host and only allowing public IPs.
    ip, err := resolveHostToIP(host)
    if err != nil || !isPublicIP(ip) {
        http.Error(w, "Host not allowed", http.StatusForbidden)
        return
    }

    // Security Decision: Use context with timeout to prevent hanging processes.
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()

    // Security Decision: Do not use shell, pass arguments directly.
    // Only allow 1 ping, no extra arguments.
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", ip)

    // Security Decision: Capture output, do not expose raw errors to user.
    output, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        http.Error(w, "Ping failed", http.StatusBadGateway)
        return
    }

    // Security Decision: Return minimal, structured response.
    resp := struct {
        Host   string `json:"host"`
        Result string `json:"result"`
    }{
        Host:   host,
        Result: "alive",
    }
    // Optionally, parse output for more detail, but never return raw output.

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidHost validates the host string using a strict regex.
func isValidHost(host string) bool {
    if len(host) == 0 || len(host) > 253 {
        return false
    }
    return allowedHost.MatchString(host)
}

// resolveHostToIP resolves a hostname to its first IPv4 address.
func resolveHostToIP(host string) (string, error) {
    ips, err := net.LookupIP(host)
    if err != nil {
        return "", err
    }
    for _, ip := range ips {
        if ip.To4() != nil {
            return ip.String(), nil
        }
    }
    return "", errors.New("no IPv4 address found")
}

// isPublicIP checks if the IP is not private, loopback, or reserved.
func isPublicIP(ipStr string) bool {
    ip := net.ParseIP(ipStr)
    if ip == nil {
        return false
    }
    // Reject loopback, private, multicast, link-local, etc.
    if ip.IsLoopback() || ip.IsPrivate() || ip.IsMulticast() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return false
    }
    return true
}
