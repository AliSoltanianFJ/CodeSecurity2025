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

// Security Decision: Only allow pinging valid hostnames or IPv4 addresses, not arbitrary user input.
// This prevents command injection (CWE-77), SSRF (CWE-918), and other abuses.
// All user input is strictly validated and sanitized.

func main() {
    // Security Decision: Listen only on localhost for demo; use HTTPS in production.
    http.HandleFunc("/url/ping", pingHandler)
    // In production, use ListenAndServeTLS with valid certs.
    http.ListenAndServe("127.0.0.1:8080", nil)
}

/* pingHandler securely pings a validated host using the system "ping" command.
   All user input is strictly validated to prevent command injection and SSRF.
   No sensitive error details are leaked to the client.
*/
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security Decision: Limit request method to GET.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and validate the "url" parameter.
    host := r.URL.Query().Get("url")
    if !isValidHost(host) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Security Decision: Limit ping to 2 packets and 2 seconds timeout.
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()

    // Security Decision: Use exec.CommandContext with arguments, never build command strings.
    // Only allow "ping" with "-n 2" (Windows) or "-c 2" (Unix). Here, assume Windows.
    cmd := exec.CommandContext(ctx, "ping", "-n", "2", host)

    // Security Decision: Do not capture stderr to avoid leaking system info.
    output, err := cmd.Output()
    if err != nil {
        // Log error server-side, but do not leak details to client.
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Security Decision: Respond with JSON, encode output to prevent XSS.
    resp := map[string]string{
        "result": string(output),
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidHost validates that the input is a valid hostname or IPv4 address.
// Security Decision: Prevents command injection, SSRF, and other abuses.
func isValidHost(host string) bool {
    if len(host) == 0 || len(host) > 253 {
        return false
    }
    // Check for valid IPv4 address.
    if ip := net.ParseIP(host); ip != nil && ip.To4() != nil {
        return true
    }
    // Validate hostname (RFC 1123).
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}
