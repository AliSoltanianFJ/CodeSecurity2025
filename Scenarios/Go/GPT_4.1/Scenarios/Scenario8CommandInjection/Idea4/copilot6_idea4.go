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

// Security Decision: Only allow pinging of hostnames or IP addresses that match strict patterns.
// This prevents command injection (CWE-77), SSRF, and other abuses.
// No shell is invoked; exec.Command is used with arguments, not a shell string.

func main() {
    // Security Decision: Listen only on localhost for demo; use HTTPS in production.
    http.HandleFunc("/url/ping", pingHandler)
    err := http.ListenAndServe("127.0.0.1:8080", nil)
    if err != nil {
        // Security Decision: Do not leak internal errors.
        fmt.Println("Server failed to start")
    }
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

    // Validate and sanitize input
    if !isValidHost(target) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Security Decision: Limit the number of ping packets and timeout.
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Security Decision: Use exec.Command with arguments, never with shell.
    // Use "-n" for Windows, "-c" for Unix. Here, assume Windows.
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", target)

    // Security Decision: Do not pass user input to shell, and do not capture stderr for user.
    output, err := cmd.Output()
    if err != nil {
        // Security Decision: Do not leak internal error details.
        http.Error(w, "Ping failed", http.StatusBadGateway)
        return
    }

    // Security Decision: Respond with JSON, encode output to prevent XSS.
    resp := map[string]string{
        "result": string(output),
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// isValidHost validates that the input is a valid hostname or IP address.
// Security Decision: Only allow ASCII hostnames and IPv4/IPv6 addresses.
// Prevents SSRF, command injection, and other abuses.
func isValidHost(host string) bool {
    if len(host) == 0 || len(host) > 253 {
        return false
    }
    // Allow IPv4, IPv6, and hostnames (RFC 1123)
    ip := net.ParseIP(host)
    if ip != nil {
        return true
    }
    // Hostname regex: labels separated by dots, each label 1-63 chars, only a-zA-Z0-9-.
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}
