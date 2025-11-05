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
// This prevents command injection (CWE-77), SSRF, and other abuses.
// No shell is invoked; exec.Command is used with arguments as a slice.

func main() {
    // Security Decision: Listen only on localhost for demo purposes.
    // In production, use HTTPS and proper authentication.
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

    // Get the "url" parameter.
    url := r.URL.Query().Get("url")
    if url == "" {
        http.Error(w, "Missing 'url' parameter", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the input.
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Security Decision: Limit the number of ping packets and timeout.
    result, err := securePing(url)
    if err != nil {
        // Do not leak internal errors to the client.
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Respond with JSON.
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]interface{}{
        "host":   url,
        "result": result,
    })
}

// isValidHost validates that the input is a valid hostname or IP address.
// Security Decision: Only allow ASCII hostnames and IPv4/IPv6 addresses.
// No ports, no special characters, no shell metacharacters.
func isValidHost(host string) bool {
    // Check if it's a valid IP address.
    if ip := net.ParseIP(host); ip != nil {
        return true
    }
    // Validate hostname: RFC 1123, max 253 chars, labels 1-63 chars, only a-zA-Z0-9-.
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    if len(host) > 253 {
        return false
    }
    return hostnameRegex.MatchString(host)
}

// securePing executes the system "ping" command in a secure way.
// Security Decision: No shell is invoked. Arguments are passed as a slice.
// Limits: 2 echo requests, 2 second timeout.
func securePing(host string) (string, error) {
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Security Decision: Use platform-specific arguments.
    // Windows: -n <count>, -w <timeout in ms>
    // Linux/macOS: -c <count>, -W <timeout in seconds>
    // For this example, assume Windows.
    cmd := exec.CommandContext(ctx, "ping", "-n", "2", "-w", "2000", host)

    // Security Decision: Do not pass user input to the shell.
    output, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        return "", errors.New("ping timeout")
    }
    if err != nil {
        return "", errors.New("ping command failed")
    }

    // Security Decision: Only return a generic result, not raw output.
    if cmd.ProcessState.Success() {
        return "host is reachable", nil
    }
    return "host is unreachable", nil
}
