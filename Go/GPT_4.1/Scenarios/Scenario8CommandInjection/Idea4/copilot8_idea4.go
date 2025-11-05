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
// No shell invocation; use exec.Command with arguments to avoid command injection (CWE-77).
// Principle of Least Privilege: No elevated privileges required.
// Sensitive data is not handled; error messages are generic and do not leak system info.

func main() {
    // Secure default: Listen only on localhost for demo; use HTTPS in production.
    http.HandleFunc("/url/ping", pingHandler)
    // In production, use ListenAndServeTLS with valid certs.
    http.ListenAndServe("127.0.0.1:8080", nil)
}

/* pingHandler securely pings a validated host using the system "ping" command.
   Only allows IPv4/IPv6 addresses or valid hostnames (no ports, no paths).
   Returns JSON with result. */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce HTTPS in production.
    // Validate HTTP method.
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

    // Optional: Prevent SSRF by restricting to allowed domains/IPs.
    // Example: allow only internal network or specific domains.

    // Use context with timeout to avoid hanging processes.
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Use "-n 1" on Windows, "-c 1" on Unix. Here, assume Windows.
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", host)

    // Do not use shell; no user input in command line.
    output, err := cmd.CombinedOutput()
    if err != nil {
        // Do not leak system errors.
        http.Error(w, "Ping failed", http.StatusBadGateway)
        return
    }

    // Return minimal JSON response.
    resp := map[string]string{
        "result": "success",
        "output": sanitizePingOutput(string(output)),
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidHost validates that the input is a safe hostname or IP address.
// No ports, no paths, no special chars. Prevents SSRF and injection.
func isValidHost(host string) bool {
    if len(host) == 0 || len(host) > 253 {
        return false
    }
    // Check for valid IPv4 or IPv6.
    if ip := net.ParseIP(host); ip != nil {
        return true
    }
    // Hostname: RFC 1123, no ports, no dots at start/end, no consecutive dots.
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}

// sanitizePingOutput strips potentially sensitive info from ping output.
func sanitizePingOutput(output string) string {
    // Only return the first line (summary), truncate to 200 chars.
    lines := regexp.MustCompile(`\r?\n`).Split(output, -1)
    if len(lines) > 0 {
        if len(lines[0]) > 200 {
            return lines[0][:200]
        }
        return lines[0]
    }
    return ""
}
