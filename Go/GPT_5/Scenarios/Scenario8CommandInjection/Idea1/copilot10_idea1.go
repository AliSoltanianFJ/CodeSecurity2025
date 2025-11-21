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

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    raw := r.URL.Query().Get("url")

    // basic presence check
    if strings.TrimSpace(raw) == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // try to parse as URL to extract host if scheme was provided
    host := raw
    if u, err := url.Parse(raw); err == nil && u.Host != "" {
        host = u.Host
    }

    // remove any port if present (handles host:port)
    if h, _, err := net.SplitHostPort(host); err == nil {
        host = h
    }

    // trim IPv6 brackets if present
    host = strings.Trim(host, "[]")

    // validate host: allow either an IP address or a hostname that matches a safe pattern
    if net.ParseIP(host) == nil {
        // simple hostname validation: labels composed of letters, digits, hyphen, separated by dots
        // and total length limit
        if len(host) > 253 {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
        hostnameRe := regexp.MustCompile(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`)
        if !hostnameRe.MatchString(host) {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
    }

    // choose platform-specific ping count flag
    var args []string
    if runtime.GOOS == "windows" {
        args = []string{"-n", "3", host}
    } else {
        args = []string{"-c", "3", host}
    }

    // run ping without a shell to avoid command injection; use a timeout context
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", args...)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}

// ...existing code...
