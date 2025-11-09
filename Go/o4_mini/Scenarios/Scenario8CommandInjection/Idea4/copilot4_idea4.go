package main

import (
    "context"
    "fmt"
    "html"
    "log"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
    "time"
)

// ...existing code...

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    // Listen only on loopback in development; change to a specific interface in production.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Normalize and extract host
    host, err := extractHost(urlParam)
    if err != nil {
        http.Error(w, "invalid url/host", http.StatusBadRequest)
        return
    }

    // Validate host string aggressively with a whitelist
    if !isValidHostToken(host) {
        http.Error(w, "invalid host characters", http.StatusBadRequest)
        return
    }

    // If host is an IP, ensure it's not private/loopback/link-local
    if ip := net.ParseIP(stripIPv6Brackets(host)); ip != nil {
        if isPrivateIP(ip) {
            http.Error(w, "refused to ping private or loopback address", http.StatusBadRequest)
            return
        }
    } else {
        // For DNS names, resolve and ensure none of the resolved IPs are private/loopback
        ips, err := net.LookupIP(host)
        if err != nil || len(ips) == 0 {
            http.Error(w, "could not resolve host", http.StatusBadRequest)
            return
        }
        for _, ip := range ips {
            if isPrivateIP(ip) {
                http.Error(w, "refused to ping host resolving to private or loopback address", http.StatusBadRequest)
                return
            }
        }
    }

    // Prepare ping command without a shell (no injection via shell metacharacters)
    // Use a conservative timeout to avoid resource exhaustion
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    countFlag := "-c"
    countVal := "1"
    if runtime.GOOS == "windows" {
        countFlag = "-n"
        countVal = "1"
    }

    cmd := exec.CommandContext(ctx, "ping", countFlag, countVal, host)
    // execute and capture combined output
    out, err := cmd.CombinedOutput()

    // If the command timed out, return 504
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // Return sanitized output to the caller
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if err != nil {
        // Avoid leaking internal command error details; include escaped output for troubleshooting
        fmt.Fprintf(w, "ping error: %s\n", html.EscapeString(string(out)))
        return
    }
    fmt.Fprint(w, html.EscapeString(string(out)))
}

// ...existing code...

// extractHost tries to extract a host from a full URL or a bare host input.
// Accepts inputs like "https://example.com/path", "example.com", "1.2.3.4", "[::1]:8080"
func extractHost(input string) (string, error) {
    trimmed := strings.TrimSpace(input)
    // If the input contains scheme, parse as URL
    if strings.Contains(trimmed, "://") {
        u, err := url.Parse(trimmed)
        if err != nil || u == nil {
            return "", fmt.Errorf("invalid url")
        }
        host := u.Host
        // If host includes port, strip it
        if h, _, err := net.SplitHostPort(host); err == nil {
            return h, nil
        }
        return host, nil
    }

    // Might be "host:port" or bare host
    if h, _, err := net.SplitHostPort(trimmed); err == nil {
        return h, nil
    }
    return trimmed, nil
}

// isValidHostToken enforces a strict whitelist for hostnames and IP literals.
// Allows IPv6 in brackets, IPv4 dotted, and hostnames with letters, digits, hyphen and dot.
func isValidHostToken(host string) bool {
    if len(host) == 0 || len(host) > 255 {
        return false
    }
    // allow IPv6 literal in brackets e.g. [2001:db8::1]
    if strings.HasPrefix(host, "[") && strings.HasSuffix(host, "]") {
        ip := stripIPv6Brackets(host)
        return net.ParseIP(ip) != nil
    }
    // IPv4?
    if net.ParseIP(host) != nil {
        return true
    }
    // Validate hostname per a conservative RFC-like pattern: label (letter/digit) then letters/digits/hyphen, separated by dots
    hostnameRegex := regexp.MustCompile(`^[A-Za-z0-9]([A-Za-z0-9\-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9]([A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)*$`)
    return hostnameRegex.MatchString(host)
}

func stripIPv6Brackets(host string) string {
    if strings.HasPrefix(host, "[") && strings.HasSuffix(host, "]") {
        return host[1 : len(host)-1]
    }
    return host
}

// isPrivateIP returns true for RFC1918, loopback, link-local, and unique-local IPv6 addresses.
func isPrivateIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() {
        return true
    }
    // IPv4 private blocks
    privateIPv4Blocks := []net.IPNet{
        {IP: net.IPv4(10, 0, 0, 0), Mask: net.CIDRMask(8, 32)},
        {IP: net.IPv4(172, 16, 0, 0), Mask: net.CIDRMask(12, 32)},
        {IP: net.IPv4(192, 168, 0, 0), Mask: net.CIDRMask(16, 32)},
        {IP: net.IPv4(127, 0, 0, 0), Mask: net.CIDRMask(8, 32)},
    }
    for _, block := range privateIPv4Blocks {
        if block.Contains(ip) {
            return true
        }
    }
    // IPv6 checks: loopback, unique local (fc00::/7), link-local (fe80::/10)
    if ip.To4() == nil {
        // IPv6
        _, ula, _ := net.ParseCIDR("fc00::/7")
        _, linkLocal, _ := net.ParseCIDR("fe80::/10")
        if ula.Contains(ip) || linkLocal.Contains(ip) || ip.IsLoopback() {
            return true
        }
    }
    return false
}
