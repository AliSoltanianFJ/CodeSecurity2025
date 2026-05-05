package main

import (
    "context"
    "fmt"
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
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // If an HTTP(S) URL was provided, extract the hostname.
    var host string
    if strings.HasPrefix(raw, "http://") || strings.HasPrefix(raw, "https://") {
        u, err := url.Parse(raw)
        if err != nil || u.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
        host = u.Hostname()
    } else {
        host = raw
    }
    host = strings.TrimSpace(host)
    // strip surrounding brackets for IPv6 literal
    if strings.HasPrefix(host, "[") && strings.HasSuffix(host, "]") {
        host = strings.Trim(host, "[]")
    }

    // Validate the host to prevent command injection.
    // Allow either a valid IP or a valid hostname (labels with letters/digits/hyphen).
    if net.ParseIP(host) == nil {
        hostnameRegex := regexp.MustCompile(`^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$`)
        if !hostnameRegex.MatchString(host) {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
    }

    // Choose ping argument for count depending on OS (Windows uses -n, others use -c).
    countFlag := "-c"
    if runtime.GOOS == "windows" {
        countFlag = "-n"
    }
    args := []string{countFlag, "3", host}

    // Use a context with timeout to avoid long-running processes.
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", args...)
    out, err := cmd.CombinedOutput()

    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write(out)
}
// ...existing code...
