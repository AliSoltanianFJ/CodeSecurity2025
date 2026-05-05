package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
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
    host := r.URL.Query().Get("url")
    if host == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // If the user provided a full URL, extract the host part
    if strings.Contains(host, "://") {
        u, err := url.Parse(host)
        if err != nil {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
        // prefer Host (which may include port); strip port later
        if u.Host != "" {
            host = u.Host
        } else {
            host = u.Path
        }
    }

    // strip optional port
    if i := strings.LastIndex(host, ":"); i != -1 {
        // only strip port if the rest doesn't contain brackets (IPv6); handle simple case
        if !strings.Contains(host, "]") {
            host = host[:i]
        }
    }

    host = strings.TrimSpace(host)
    if host == "" {
        http.Error(w, "empty host after parsing", http.StatusBadRequest)
        return
    }

    if !isSafeHost(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Run ping without invoking a shell and with a timeout to avoid hangs.
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    // Use an argument list (no shell interpolation). On Windows the flag differs; detect by OS if necessary.
    // For portability choose common Linux/mac approach here. Adjust as needed for Windows (-n instead of -c).
    cmd := exec.CommandContext(ctx, "ping", "-c", "4", host)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}

func isSafeHost(h string) bool {
    // Disallow characters that would indicate command injection or path parts.
    if strings.ContainsAny(h, "/\\;|&$<>`") {
        return false
    }
    // Accept plain IPv4/IPv6 addresses
    if ip := net.ParseIP(h); ip != nil {
        return true
    }
    // Validate hostname: labels separated by dots, letters/numbers and hyphens, no leading/trailing hyphen
    // Very permissive but blocks most malicious characters.
    re := regexp.MustCompile(`^[a-zA-Z0-9]([a-zA-Z0-9\-\.]*[a-zA-Z0-9])?$`)
    return re.MatchString(h)
}
