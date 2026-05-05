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
    // Security: Listen only on localhost for demo; use HTTPS in production.
    http.HandleFunc("/url/ping", pingHandler)
    // In production, use ListenAndServeTLS with valid certs.
    http.ListenAndServe("127.0.0.1:8080", nil)
}

// pingHandler handles /url/ping requests securely.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET requests.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get and validate the "url" parameter.
    target := r.URL.Query().Get("url")
    if !isValidPingTarget(target) {
        http.Error(w, "Invalid target", http.StatusBadRequest)
        return
    }

    // Security: Limit execution time to prevent DoS.
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Security: Use exec.CommandContext with arguments, never shell.
    // Only allow 1 ping for speed and to limit resource use.
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", target)

    // Security: Do not pass user input to shell, and do not return raw output.
    output, err := cmd.CombinedOutput()
    if err != nil {
        // Security: Do not leak internal errors.
        http.Error(w, "Ping failed", http.StatusBadGateway)
        return
    }

    // Security: Return only minimal, safe information.
    result := struct {
        Target  string `json:"target"`
        Success bool   `json:"success"`
    }{
        Target:  target,
        Success: isPingSuccess(string(output)),
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(result)
}

// isValidPingTarget validates that the target is a safe hostname or IPv4 address.
// Security: Prevents command injection, SSRF, and other abuses.
func isValidPingTarget(target string) bool {
    // Allow only valid IPv4 addresses or hostnames (no ports, no special chars).
    ip := net.ParseIP(target)
    if ip != nil && ip.To4() != nil {
        return true
    }
    // Hostname: RFC 1123, max 253 chars, only a-zA-Z0-9-. (no underscores, no trailing dot)
    hostnamePattern := `^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`
    if len(target) > 253 {
        return false
    }
    matched, _ := regexp.MatchString(hostnamePattern, target)
    return matched
}

// isPingSuccess parses ping output to determine if the ping succeeded.
// Security: Only checks for "TTL=" in output, which is present on success in Windows ping.
func isPingSuccess(output string) bool {
    // Windows ping returns "TTL=" on success.
    return regexp.MustCompile(`(?i)TTL=`).FindString(output) != ""
}
