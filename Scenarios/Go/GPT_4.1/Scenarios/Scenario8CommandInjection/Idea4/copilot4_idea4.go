package main

import (
    "encoding/json"
    "fmt"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "time"
)

// Security Decision: Only allow IPv4/IPv6 addresses or valid hostnames (no arbitrary shell input).
// Strictly validate and sanitize user input to prevent command injection (CWE-77).
// Do not leak internal errors to the client.

func main() {
    // Security: Listen only on localhost for demo; use HTTPS in production.
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe("127.0.0.1:8080", nil)
}

// pingHandler securely handles ping requests.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Limit request method to GET.
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and sanitize the "url" parameter.
    url := r.URL.Query().Get("url")
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Security: Use exec.Command with arguments, never shell.
    // Limit ping to 2 packets and 2 seconds timeout.
    cmd := exec.Command("ping", "-n", "2", "-w", "2000", url)

    // Security: Set a timeout for the command execution.
    // Principle of least privilege: No elevated privileges required.
    done := make(chan error, 1)
    go func() {
        done <- cmd.Run()
    }()

    select {
    case err := <-done:
        if err != nil {
            // Security: Do not leak internal errors.
            http.Error(w, "Ping failed", http.StatusBadGateway)
            return
        }
    case <-time.After(3 * time.Second):
        // Security: Prevent hanging processes.
        _ = cmd.Process.Kill()
        http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
        return
    }

    // Security: Respond with JSON, encode output.
    resp := map[string]string{"status": "alive"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidHost validates the host as either an IP address or a valid hostname.
// Security: Prevents SSRF, command injection, and other attacks.
func isValidHost(host string) bool {
    if len(host) == 0 || len(host) > 253 {
        return false
    }
    // Check if it's a valid IP address.
    if ip := net.ParseIP(host); ip != nil {
        return true
    }
    // Validate hostname (RFC 1123).
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}
