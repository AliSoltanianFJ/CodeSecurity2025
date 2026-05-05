package main

import (
    "fmt"
    "html"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    url := r.URL.Query().Get("url")

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Sanitize input: Remove any unwanted characters (defense in depth)
    safeURL := html.EscapeString(url)

    // Use exec.Command with arguments, not shell, to avoid command injection
    cmd := exec.Command("ping", "-n", "1", safeURL)

    // Set a timeout for the command to prevent hanging
    var output []byte
    done := make(chan error, 1)
    go func() {
        var err error
        output, err = cmd.CombinedOutput()
        done <- err
    }()

    select {
    case err := <-done:
        if err != nil {
            http.Error(w, "Ping failed", http.StatusInternalServerError)
            return
        }
    case <-time.After(3 * time.Second):
        cmd.Process.Kill()
        http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
        return
    }

    // Prevent XSS by escaping output
    fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(string(output)))
}

// isValidHost validates that the input is a valid hostname or IP address
func isValidHost(host string) bool {
    // Check for empty input
    if len(host) == 0 || len(host) > 253 {
        return false
    }
    // Check if it's a valid IP address
    if net.ParseIP(host) != nil {
        return true
    }
    // Validate hostname using regex (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    if !hostnameRegex.MatchString(host) {
        return false
    }
    // Prevent SSRF: Only allow certain domains or block internal IPs if needed
    // Example: block localhost and private IPs
    lowerHost := strings.ToLower(host)
    if lowerHost == "localhost" || strings.HasSuffix(lowerHost, ".local") {
        return false
    }
    ip := net.ParseIP(host)
    if ip != nil && (ip.IsLoopback() || ip.IsPrivate()) {
        return false
    }
    return true
}
