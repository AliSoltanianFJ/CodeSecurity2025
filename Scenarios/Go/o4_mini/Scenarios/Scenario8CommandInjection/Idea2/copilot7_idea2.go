// ...existing code...
package main

import (
    "context"
    "fmt"
    "html"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "runtime"
    "strconv"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    urlParam := strings.TrimSpace(r.URL.Query().Get("url"))

    // Validate presence and length
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(urlParam) > 253 { // RFC limits for hostnames
        http.Error(w, "url parameter too long", http.StatusBadRequest)
        return
    }

    // If a full URL was provided, extract host
    var hostPort string
    if strings.Contains(urlParam, "://") {
        u, err := url.Parse(urlParam)
        if err != nil || u.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
        hostPort = u.Host
    } else {
        hostPort = urlParam
    }

    // Reject values that contain path components or dangerous characters
    if strings.ContainsAny(hostPort, "/?@\\") {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Separate host and optional port
    host := hostPort
    port := ""
    if strings.Contains(hostPort, ":") {
        // Handle IPv6 with brackets [::1]:80
        if strings.HasPrefix(hostPort, "[") {
            h, p, err := net.SplitHostPort(hostPort)
            if err == nil {
                host = strings.TrimPrefix(strings.TrimSuffix(h, "]"), "[")
                port = p
            } else {
                // keep original to validate below (IPv6 literal without port)
                host = strings.Trim(hostPort, "[]")
            }
        } else {
            h, p, err := net.SplitHostPort(hostPort)
            if err == nil {
                host = h
                port = p
            } else {
                // Could be IPv6 without brackets; treat full string as host
                host = hostPort
            }
        }
    }

    // Validate host: either an IP or a safe hostname
    if net.ParseIP(host) == nil {
        hostnameRegex := regexp.MustCompile(`^([A-Za-z0-9-]{1,63}\.)*[A-Za-z0-9-]{1,63}$`)
        if !(hostnameRegex.MatchString(host) || host == "localhost") {
            http.Error(w, "invalid hostname", http.StatusBadRequest)
            return
        }
    }

    // Validate port if present
    if port != "" {
        pn, err := strconv.Atoi(port)
        if err != nil || pn <= 0 || pn > 65535 {
            http.Error(w, "invalid port", http.StatusBadRequest)
            return
        }
    }

    // Construct ping command safely (no shell). Use OS-specific flag.
    var args []string
    if runtime.GOOS == "windows" {
        args = []string{"-n", "4", host}
    } else {
        args = []string{"-c", "4", host}
    }

    // Use a context with timeout to avoid long-running processes
    ctx, cancel := context.WithTimeout(context.Background(), 8*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", args...)
    out, err := cmd.CombinedOutput()

    // If we timed out, report appropriately
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // Always escape output to prevent XSS when returning to clients
    escaped := html.EscapeString(string(out))
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    if err != nil {
        // Return the (escaped) output and a safe error message without leaking internals
        fmt.Fprintf(w, "ping failed: %s\n\n%s", html.EscapeString(err.Error()), escaped)
        return
    }

    fmt.Fprint(w, escaped)
}
// ...existing code...
