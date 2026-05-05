// ...existing code...
package main

import (
    "fmt"
    "net"
    "net/http"
    "net/url"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive without invoking shell commands */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse the input safely. If scheme missing, add http:// to help parsing.
    parsed, err := url.Parse(urlStr)
    if err != nil || parsed.Host == "" {
        parsed, err = url.Parse("http://" + urlStr)
        if err != nil {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Extract host (may include port). If empty, fallback to Path.
    hostPort := parsed.Host
    if hostPort == "" {
        hostPort = parsed.Path
    }
    // Remove any trailing path components if present
    if i := strings.Index(hostPort, "/"); i >= 0 {
        hostPort = hostPort[:i]
    }

    // Try to separate host and port; keep hostOnly for DNS lookup
    hostOnly := hostPort
    if h, _, err := net.SplitHostPort(hostPort); err == nil {
        hostOnly = h
    }
    // For addresses like "example.com:80" or "[::1]:80" handled above.
    if hostOnly == "" {
        http.Error(w, "could not determine host", http.StatusBadRequest)
        return
    }

    // Resolve the host to one or more IPs
    ips, err := net.LookupIP(hostOnly)
    if err != nil || len(ips) == 0 {
        http.Error(w, fmt.Sprintf("host lookup failed: %v", err), http.StatusBadRequest)
        return
    }

    // Try a TCP connect to common ports to determine reachability (avoids shell/ICMP)
    timeout := 2 * time.Second
    reachable := false
    var lastErr error
    for _, ip := range ips {
        for _, port := range []string{"80", "443"} {
            addr := net.JoinHostPort(ip.String(), port)
            conn, err := net.DialTimeout("tcp", addr, timeout)
            if err == nil {
                conn.Close()
                reachable = true
                break
            }
            lastErr = err
        }
        if reachable {
            break
        }
    }

    w.Header().Set("Content-Type", "text/plain")
    if reachable {
        fmt.Fprintf(w, "host %s is reachable\n", hostOnly)
    } else {
        fmt.Fprintf(w, "host %s is not reachable (last error: %v)\n", hostOnly, lastErr)
    }
}
// ...existing code...
